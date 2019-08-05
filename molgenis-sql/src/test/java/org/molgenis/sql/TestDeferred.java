package org.molgenis.sql;

import org.junit.Assert;
import org.junit.Test;
import org.molgenis.*;
import org.molgenis.Row;
import org.molgenis.utils.StopWatch;

import java.util.UUID;

import static junit.framework.TestCase.fail;

public class TestDeferred {
  Database database = DatabaseFactory.getDatabase("molgenis", "molgenis");

  public TestDeferred() throws MolgenisException {}

  public void DependencyOrderNotNeededInTransaction() throws MolgenisException {

    StopWatch.start("DependencyOrderNotNeededInTransaction");
    try {
      database.transaction(
          db -> {
            runTestCase(db);

            StopWatch.print("data added (in wrong dependency order, how cool is that??)");
          });
    } catch (Exception e) {
      fail("should not fail, dependency order should be deferred");
    }
    StopWatch.print("transaction committed)");
  }

  @Test(expected = MolgenisException.class)
  public void DependencyOrderOutsideTransactionFails() throws MolgenisException {
    runTestCase(database);
  }

  public void runTestCase(Database db) throws MolgenisException {
    Schema schema = db.createSchema("TestDeffered");

    Table subjectTable = schema.createTableIfNotExists("Subject");

    Table sampleTable = schema.createTableIfNotExists("Sample");
    sampleTable.addRef("subject", "Subject");

    StopWatch.print("schema created");

    org.molgenis.Row aSubjectRow = new Row();
    org.molgenis.Row aSampleRow = new Row().setUuid("subject", aSubjectRow.getMolgenisid());

    sampleTable.insert(aSampleRow);
    subjectTable.insert(aSubjectRow);
  }

  @Test
  public void foreignKeysInTransactionsAraProtected() throws MolgenisException {
    StopWatch.start("foreignKeysInTransactionsAraProtected");

    try {
      database.transaction(
          db -> {
            Schema schema = db.createSchema("TestDeffered3");

            Table subjectTable = schema.createTableIfNotExists("Subject");

            Table sampleTable = schema.createTableIfNotExists("Sample");
            sampleTable.addRef("subject", "Subject");

            StopWatch.print("schema created");

            org.molgenis.Row subject1 = new Row();
            org.molgenis.Row sample1 = new Row().setUuid("subject", UUID.randomUUID());
            org.molgenis.Row sample2 = new Row().setUuid("subject", UUID.randomUUID());

            sampleTable.insert(sample1, sample2);
            subjectTable.insert(subject1);

            StopWatch.print("data added");
          });
      StopWatch.print("transaction committed)");
      fail("should have failed on wrong fkey");
    } catch (MolgenisException e) {
      StopWatch.print("errored correctly " + e.getCause().getMessage());
    }
  }
}
