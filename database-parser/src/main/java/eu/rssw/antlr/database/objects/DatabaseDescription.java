package eu.rssw.antlr.database.objects;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class DatabaseDescription {
  private String dbName;
  private Map<String, Sequence> sequences = new HashMap<>();
  private Map<String, Table> tables = new HashMap<>();

  public DatabaseDescription(String dbName) {
    this.dbName = dbName;
  }

  public String getDbName() {
    return dbName;
  }

  public Collection<Sequence> getSequences() {
    return sequences.values();
  }

  public Sequence getSequence(String name) {
    return sequences.get(name);
  }

  public Collection<Table> getTables() {
    return tables.values();
  }

  public Table getTable(String name) {
    return tables.get(name);
  }

  public void addTable(Table tbl) {
    tables.put(tbl.getName(), tbl);
  }

  public void addSequence(Sequence seq) {
    sequences.put(seq.getName(), seq);
  }

  public void serialize(OutputStream out) throws IOException {
    try (OutputStreamWriter osw = new OutputStreamWriter(out, Charset.forName("utf-8"));
        BufferedWriter writer = new BufferedWriter(osw)) {
      for (Sequence s : sequences.values()) {
        writer.write("S" + s.getName());
        writer.newLine();
      }
      for (Table t : tables.values()) {
        writer.write("T" + t.getName());
        writer.newLine();
        for (Field f : t.getFields()) {
          writer.write("F" + f.getName() + ":" + f.getDataType() + ":" + f.getExtent());
          writer.newLine();
        }
      }
    }
  }

  public static DatabaseDescription deserialize(InputStream in, String name) throws IOException {
    DatabaseDescription db = new DatabaseDescription(name);
    try (InputStreamReader isr = new InputStreamReader(in); BufferedReader reader = new BufferedReader(isr)) {
      String line = null;
      Table currTbl = null;
      while ((line = reader.readLine()) != null) {
        if (line.startsWith("S")) {
          db.addSequence(new Sequence(line.substring(1)));
        } else if (line.startsWith("T")) {
          currTbl = new Table(line.substring(1));
          db.addTable(currTbl);
        } else if (line.startsWith("F")) {
          // FieldName:DataType:Extent
          int ch1 = line.indexOf(':');
          int ch2 = line.lastIndexOf(':');
          if ((currTbl == null) || (ch1 == -1) || (ch2 == -1))
            throw new IOException("Invalid file format: " + line);

          Field f = new Field(line.substring(1, ch1), line.substring(ch1 + 1, ch2));
          f.setExtent(Integer.parseInt(line.substring(ch2 + 1)));
          currTbl.addField(f);

        }
      }
    }

    return db;
  }
}
