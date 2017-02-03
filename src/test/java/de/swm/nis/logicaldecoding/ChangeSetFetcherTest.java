/*
Copyright (c) 2016 Sebastian Schmidt

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
package de.swm.nis.logicaldecoding;

import de.swm.nis.logicaldecoding.dataaccess.ChangeSetDAO;
import de.swm.nis.logicaldecoding.dataaccess.ChangeSetFetcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.when;

public class ChangeSetFetcherTest
{
    @Mock
    JdbcTemplate jdbctemplate;

    @Before
    public void setUp()
            throws Exception
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testFetchChanges()
    {
        List<ChangeSetDAO> changes = new ArrayList<ChangeSetDAO>();

        ChangeSetDAO item1 = new ChangeSetDAO();
        item1.setData("dummy data");
        item1.setLocation("X234asf");
        item1.setTransactionId(12345);
        changes.add(item1);

        when(jdbctemplate.query(eq("SELECT * from pg_logical_slot_get_changes(?, NULL, ?, 'include-timestamp', 'on')"),
                refEq(new Object[] {"testslot", 100}), Matchers.<RowMapper<ChangeSetDAO>>any())).thenReturn(changes);

        ChangeSetFetcher fetcher = new ChangeSetFetcher(jdbctemplate);
        List<ChangeSetDAO> fetchedChanges = fetcher.fetch("testslot", 100);

        assertEquals(changes.size(), fetchedChanges.size());
        assertEquals(changes.size(), 1);
        assertEquals(changes.get(0), fetchedChanges.get(0));
    }

    @Test
    public void testPeekChanges()
    {
        when(
                jdbctemplate.queryForObject(eq("SELECT count(*) from pg_logical_slot_peek_changes(?, NULL, NULL)"),
                        refEq(new Object[] {"testslot"}), eq(Long.class))).thenReturn(100L);

        ChangeSetFetcher fetcher = new ChangeSetFetcher(jdbctemplate);
        Long numChanges = fetcher.peek("testslot");
        assertEquals("Expected a num of 100 available changes", numChanges, (Long) 100L);
    }
}
