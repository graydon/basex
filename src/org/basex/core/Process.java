package org.basex.core;

import static org.basex.Text.*;
import java.io.IOException;
import org.basex.BaseX;
import org.basex.data.Data;
import org.basex.data.Nodes;
import org.basex.data.Result;
import org.basex.io.PrintOutput;
import org.basex.query.QueryException;
import org.basex.query.QueryProcessor;
import org.basex.util.Performance;
import org.basex.util.TokenBuilder;

/**
 * This class provides the architecture for all internal command
 * implementations. It evaluates queries that are sent by the GUI, the client or
 * the standalone version.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-09, ISC License
 * @author Christian Gruen
 */
public abstract class Process extends Progress {
  /** Commands flag: standard. */
  protected static final int STANDARD = 0;
  /** Commands flag: printing command. */
  protected static final int PRINTING = 1;
  /** Commands flag: updating command. */
  protected static final int UPDATING = 2;
  /** Commands flag: data reference needed. */
  protected static final int DATAREF = 4;

  /** Command arguments. */
  public String[] args;
  /** Database context. */
  protected Context context;
  /** Database properties. */
  protected Prop prop;

  /** Container for query information. */
  protected TokenBuilder info = new TokenBuilder();
  /** Performance measurements. */
  protected Performance perf = new Performance();
  /** Temporary query result. */
  protected Result result;
  /** Command properties. */
  private final int props;

  /**
   * Constructor.
   * @param p properties
   * @param a arguments
   */
  public Process(final int p, final String... a) {
    props = p;
    args = a;
  }

  /**
   * Executes the process and serializes the results. If an error happens, an
   * exception is thrown.
   * @param ctx query context
   * @param out output stream
   * @throws Exception execution exception
   */
  public void execute(final Context ctx, final PrintOutput out)
      throws Exception {

    if(!execute(ctx)) throw new RuntimeException(info());
    output(out);
    if(ctx.prop.is(Prop.INFO)) out.print(info());
  }

  /**
   * Executes the process and returns a success flag.
   * @param ctx query context
   * @return success flag
   */
  public final boolean execute(final Context ctx) {
    context = ctx;
    prop = ctx.prop;
    
    final Data data = context.data();
    // data reference needed?
    if(data() && data == null) return error(PROCNODB);

    if(data != null) {
      // wait until update commands have been completed..
      while(data.getLock() == 2) Performance.sleep(50);
      // check update commands..
      if(updating()) {
        if(prop.is(Prop.TABLEMEM) || prop.is(Prop.MAINMEM))
          return error(PROCMM);
        while(data.getLock() != 0) Performance.sleep(50);
      }
    }

    try {
      if(data != null) data.setLock(updating() ? 2 : 1);
      final boolean ok = exec();
      if(data != null && !printing()) data.setLock(0);
      return ok;
    } catch(final Throwable ex) {
      if(data != null) data.setLock(0);

      // not expected...
      ex.printStackTrace();
      if(ex instanceof OutOfMemoryError) {
        Performance.gc(2);
        return error(PROCOUTMEM);
      }
      return error(PROCERR, this, ex.toString());
    }
  }

  /**
   * Executes a process.
   * @return success of operation
   */
  protected boolean exec() {
    return true;
  }

  /**
   * Serializes the textual results.
   * @param out output stream
   * @throws IOException I/O exception
   */

  public final void output(final PrintOutput out) throws IOException {
    final Data data = context.data();
    try {
      if(printing()) out(out);
    } catch(final IOException ex) {
      if(data != null) data.setLock(0);
      throw ex;
    } catch(final Exception ex) {
      out.print(ex.toString());
      BaseX.debug(ex);
    }
    if(data != null) data.setLock(0);
  }

  /**
   * Returns a query result.
   * @param out output stream
   * @throws IOException exception
   */
  @SuppressWarnings("unused")
  protected void out(final PrintOutput out) throws IOException {
  }

  /**
   * Returns process info.
   * @param out output stream
   * @throws IOException I/O exception
   */
  public final void info(final PrintOutput out) throws IOException {
    out.print(info.toString());
  }

  /**
   * Adds the error message to the message buffer {@link #info}.
   * @param msg error message
   * @param ext error extension
   * @return false
   */
  public final boolean error(final String msg, final Object... ext) {
    info.reset();
    info.add(msg == null ? "" : msg, ext);
    info.add(NL);
    return false;
  }

  /**
   * Adds information on the process execution.
   * @param str information to be added
   * @param ext extended info
   * @return true
   */
  public final boolean info(final String str, final Object... ext) {
    if(prop.is(Prop.INFO)) {
      info.add(str, ext);
      info.add(NL);
    }
    return true;
  }

  /**
   * Returns the query information as a string.
   * @return info string
   */
  public final String info() {
    return info.toString();
  }

  /**
   * Returns the result set, generated by the last query.
   * @return result set
   */
  public final Result result() {
    return result;
  }

  /**
   * Performs the specified XQuery.
   * @param q query to be performed
   * @param err if this string is specified, it is thrown if the results don't
   *          yield element nodes
   * @return result set
   */
  protected final Nodes query(final String q, final String err) {
    try {
      final String query = q == null ? "" : q;
      final QueryProcessor qu = new QueryProcessor(query, context);
      progress(qu);
      final Nodes nodes = qu.queryNodes();
      // check if all result nodes are tags
      if(err != null) {
        final Data data = context.data();
        for(int i = nodes.size() - 1; i >= 0; i--) {
          if(data.kind(nodes.nodes[i]) != Data.ELEM) {
            error(err);
            return null;
          }
        }
      }
      return nodes;
    } catch(final QueryException ex) {
      BaseX.debug(ex);
      error(ex.getMessage());
      return null;
    }
  }

  /**
   * Returns if the current command yields some output.
   * @return result of check
   */
  public final boolean printing() {
    return check(PRINTING);
  }

  /**
   * Returns if the current command needs a data reference for processing.
   * @return result of check
   */
  public final boolean data() {
    return check(DATAREF);
  }

  /**
   * Returns if the current command generates updates in the data structure.
   * @return result of check
   */
  public final boolean updating() {
    return check(UPDATING);
  }

  /**
   * Checks the specified command property.
   * @param pr property to be checked
   * @return result of check
   */
  private boolean check(final int pr) {
    return (props & pr) != 0;
  }

  /**
   * Executes the specified process and adopts the process results to the
   * current process.
   * @param proc process to be executed
   * @return success of operation
   */
  protected final boolean exec(final Process proc) {
    progress(proc);
    final boolean ok = proc.execute(context);
    info = proc.info;
    return ok;
  }

  /**
   * Returns the length of the longest string.
   * @param str strings
   * @return maximum length
   */
  protected static int maxLength(final String[] str) {
    int max = 0;
    for(final String s : str)
      if(max < s.length()) max = s.length();
    return max;
  }

  /**
   * Returns the list of arguments.
   * @return arguments
   */
  public final String args() {
    final StringBuilder sb = new StringBuilder();
    for(final String a : args)
      if(a != null) sb.append(" " + a);
    return sb.toString();
  }

  /**
   * Returns the class name.
   * @return class name
   */
  public final String name() {
    return getClass().getSimpleName().toUpperCase();
  }

  @Override
  public String toString() {
    return name() + args();
  }
}
