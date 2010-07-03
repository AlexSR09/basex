package org.basex.examples.perf;

import static java.lang.System.*;
import org.basex.core.Context;
import org.basex.core.LocalSession;
import org.basex.core.Main;
import org.basex.core.Session;
import org.basex.core.cmd.Check;
import org.basex.core.cmd.DropDB;
import org.basex.core.cmd.Set;
import org.basex.core.cmd.XQuery;
import org.basex.io.CachedOutput;
import org.basex.server.ClientSession;
import org.basex.util.Args;

/**
 * This class benchmarks delete operations.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
 * @author BaseX Team
 */
public abstract class Benchmark {
  /** Session. */
  private Session session;
  /** Performance.
  private final Performance perf = new Performance();
  */

  /** Input document. */
  private String input;
  /** Number of runs. */
  private int runs = 1;
  /** Maximum number of milliseconds to wait for any query. */
  private int totalMax = Integer.MAX_VALUE;
  /** Maximum number of milliseconds to wait for a single query. */
  private int max = Integer.MAX_VALUE;
  /** Local vs server flag. */
  private boolean local;

  /**
   * Private constructor.
   * @param args command-line arguments
   * @return result flag
   * @throws Exception exception
   */
  protected boolean init(final String... args) throws Exception {
    out.println("=== " + Main.name(this) + " Test ===\n");
    if(!parseArguments(args)) return false;

    final Context ctx = new Context();
    session = local ? new LocalSession(ctx) :
      new ClientSession(ctx, "admin", "admin");
    
    // create test database
    session.execute(new Set("info", "all"));

    drop();
    return true;
  }

  /**
   * Opens the test database.
   * @throws Exception exception
   */
  protected void check() throws Exception {
    session.execute(new Check(input));
  }

  /**
   * Drops the test database.
   * @throws Exception exception
   */
  protected void drop() throws Exception {
    session.execute(new DropDB(Main.name(this)));
  }

  /**
   * Creates a new database instance and performs a query.
   * @param queries queries to be evaluated
   * @throws Exception exception
   */
  protected void update(final String... queries) throws Exception {
    update(1, queries);
  }
  
  /**
   * Creates a new database instance and performs a query for the
   * specified number of runs.
   * @param queries queries to be evaluated
   * @param r runs the number for the specified number of time without creating
   *   a new database
   * @throws Exception exception
   */
  protected void update(final int r, final String... queries) throws Exception {
    if(queries.length == 0) return;

    out.print("* Queries: " + queries[0]);
    if(queries.length > 1)out.print(", ...");
    if(r > 1) out.print(" (" + r + "x)");
    out.println();

    // minimum time for performing all queries
    double time = Double.MAX_VALUE;
    // number of updated nodes
    int upd = 0;

    // loop through global number of runs
    for(int rr = 0; rr < runs; rr++) {
      upd = 0;
      double t = 0;
      check();

      // loop through all queries
      for(final String q : queries) {
        // loop through number of runs for a single query
        for(int rn = 0; rn < r; rn++) {
          session.execute(new XQuery(q));

          final String inf = session.info().replaceAll("\\r?\\n", " ");
          // get number of updated nodes
          upd += Long.parseLong(inf.replaceAll(".*Updated: ([^ ]+).*", "$1"));
          // get execution time
          t += Double.parseDouble(inf.replaceAll(".*Time: ([^ ]+).*", "$1"));
          if(t > totalMax) break;
        }
        if(t > totalMax) break;
      }
      //drop();

      if(time > t) time = t;
      if(t > totalMax) {
        time = -1;
        break;
      }
      if(t > max) break;
    }
    out.println("  Nodes: " + upd);
    out.println("  ms: " + Math.round(time));
  }

  /**
   * Performs the specified query and returns the result.
   * @param query query to be evaluated
   * @return result
   * @throws Exception exception
   */
  protected String query(final String query) throws Exception {
    check();
    final CachedOutput co = new CachedOutput();
    session.execute(new XQuery(query), co);
    return co.toString();
  }

  /**
   * Parses the command-line arguments.
   * @param args command-line arguments
   * @return true if all arguments have been correctly parsed
   */
  protected boolean parseArguments(final String[] args) {
    final Args arg = new Args(args, this,
        " [-lr] document\n" + 
        " -l        use local session\n" +
        " -m<max>   maximum #ms for a single query\n" +
        " -M<max>   total maximum #ms\n" +
        " -r<runs>  number of runs");

    while(arg.more()) {
      if(arg.dash()) {
        final char ch = arg.next();
        if(ch == 'r') {
          runs = arg.num();
          out.println("- number of runs: " + runs);
        } else if(ch == 'l') {
          local = true;
          out.println("- local session");
        } else if(ch == 'm') {
          max = arg.num();
          out.println("- maximum #ms: " + max);
        } else if(ch == 'M') {
          totalMax = arg.num();
          out.println("- total maximum #ms: " + totalMax);
        } else {
          arg.check(false);
        }
      } else {
        input = arg.string();
      }
    }
    if(input == null) {
      arg.check(false);
    } else {
      out.println("- Document: " + input);
      out.println();
    }
    return arg.finish();
  }
}
