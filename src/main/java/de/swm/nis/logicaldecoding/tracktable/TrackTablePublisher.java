package de.swm.nis.logicaldecoding.tracktable;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Repository;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.WKBWriter;

import de.swm.nis.logicaldecoding.parser.Cell;
import de.swm.nis.logicaldecoding.parser.Row;

@Repository
public class TrackTablePublisher {
	
	private static final Logger log = LoggerFactory.getLogger(TrackTablePublisher.class);
	
	@Autowired
	private JdbcTemplate template;
	
	@Value("${tracktable.schemaname}")
	private String schemaname;
	
	@Value("${tracktable.tablename}")
	private String tableName;
	
	@Value("#{'${tracktable.metainfo.searchpatterns}'.split(',')}")
	private List<String> metaInfoSearchPatterns;
	
	@Async
	public Future<String> publish(Collection<Row> rows) {
		log.info("Publishing " + rows.size()+" change metadata to track table");
		for(Row row:rows) {
			publish(row);
		}
		return new AsyncResult<String>("success");
	}
	
	public void publish(Row row) {
		Envelope envelope = row.getEnvelope();
		GeometryFactory geomFactory = new GeometryFactory(new PrecisionModel(), 31468);
		WKBWriter wkbWriter = new WKBWriter(2, true);
		
		//TODO the next statement does not work with empty envelopes (ie no geometry columns found or so).
		byte[] wkb = wkbWriter.write(geomFactory.toGeometry(envelope));
		String metadata = extractMetadata(row);
		String changedTableSchema = row.getSchemaName();
		String changedTableName = Iterables.get(Splitter.on('.').split(row.getTableName()),1);
		String type = row.getType().toString();
		
		Object[] params = new Object[]{wkb, type, changedTableSchema, changedTableName, metadata};
		int[] types = new int[] {Types.BINARY, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,Types.VARCHAR};
		
		String sql = "INSERT INTO "+schemaname + "." + tableName + "(region, type, schema, tablename, metadata) VALUES (?,?,?,?,?)";
		template.update(sql, params,types);
		
	}
	
	private String extractMetadata(Row row) {
		switch(row.getType()) {
			case delete:
			{
				return extractMetadata(row.getOldValues());
			}
			default:
			{
				return extractMetadata(row.getNewValues());
			}
			
		}
	}
	
	
	private String extractMetadata(Collection<Cell> cells) {
		List<String> parts = new ArrayList<String>();
		for (Cell cell:cells) {
			for (String pattern:metaInfoSearchPatterns) {
			if (cell.getName().startsWith(pattern) || cell.getName().endsWith(pattern)) {
				parts.add(new String(cell.getName() + ": " + cell.getValue()));
				}
			}
		}
		return Joiner.on(", ").join(parts);
	}
	
	
}