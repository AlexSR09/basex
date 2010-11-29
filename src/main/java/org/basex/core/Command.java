package org.basex.core;

import static org.basex.core.Text.*;
import java.io.IOException;
import java.io.OutputStream;
import org.basex.core.Commands.CmdPerm;
import org.basex.core.cmd.Close;
import org.basex.data.Data;
import org.basex.data.Result;
import org.basex.io.ArrayOutput;
import org.basex.io.NullOutput;
import org.basex.io.PrintOutput;
import org.basex.util.Performance;
import org.basex.util.TokenBuilder;
import org.basex.util.Util;

/**
 * This class provides the architecture for all internal command
 * implementations. It evaluates queries that are sent by the GUI, the client or
 * the standalone version.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
 * @author Christian Gruen
 */
public abstract class Command extends Progress {
  /** Commands flag: standard. */
  public static final int STANDARD = 256;
  /** Commands flag: data reference needed. */
  public static final int DATAREF = 512;

  /** Container for query information. */
  private final TokenBuilder info = new TokenBuilder();
  /** Command arguments. */
  protected final String[] args;

  /** Performance measurements. */
  protected Performance perf;
  /** Database context. */
  protected Context context;
  /** Output stream. */
  protected PrintOutput out;
  /** Database properties. */
  protected Prop prop;

  /** Flags for controlling command processing. */
  private final int flags;

  /**
   * Constructor.
   * @param f command flags
   * @param a arguments
   */
  public Command(final int f, final String... a) {
    flags = f;
    args = a;
  }

  /**
   * Executes the command and serializes textual results to the specified output
   * stream. If an exception occurs, a {@link BaseXException} is thrown.
   * @param ctx database context
   * @param os output stream reference
   * @throws BaseXException command exception
   */
  public final void execute(final Context ctx, final OutputStream os)
      throws BaseXException {
    if(!exec(ctx, os)) throw new BaseXException(info());
  }

  /**
   * Executes the command and returns the result as string.
   * If an exception occurs, a {@link BaseXException} is thrown.
   * @param ctx database context
   * @return string result
   * @throws BaseXException command exception
   */
  public final String execute(final Context ctx) throws BaseXException {
    final ArrayOutput ao = new ArrayOutput();
    execute(ctx, ao);
    return ao.toString();
  }

  /**
   * Runs the command without permission, data and concurrency checks.
   * Should be called with care, and only by other database commands.
   * @param ctx query context
   * @return result of check
   */
  public final boolean run(final Context ctx) {
    return run(ctx, new NullOutput());
  }

  /**
   * Returns command information.
   * @return info string
   */
  public final String info() {
    return info.toString();
  }

  /**
   * Returns the result set, generated by a query command.
   * Will only yield results if {@link Prop#CACHEQUERY} is set.
   * @return result set
   */
  public Result result() {
    return null;
  }

  /**
   * Checks if the command performs updates/write operations.
   * @param ctx context reference
   * @return result of check
   */
  @SuppressWarnings("unused")
  public boolean updating(final Context ctx) {
    return (flags & (User.CREATE | User.WRITE)) != 0;
  }

  /**
   * Checks if the command updates the data reference.
   * @return result of check
   */
  public boolean newData() {
    return false;
  }

  /**
   * Returns true if this class returns a progress value.
   * Used by the progress bar in the visualization.
   * @return result of check
   */
  public boolean supportsProg() {
    return false;
  }

  /**
   * Returns true if this command can be stopped.
   * Used by the progress bar in the visualization.
   * @return result of check
   */
  public boolean stoppable() {
    return false;
  }

  /**
   * Builds a string representation from the command. This string must be
   * correctly built, as commands are sent to the server as strings.
   * @param cb command builder
   */
  public void build(final CommandBuilder cb) {
    cb.init().args();
  }

  @Override
  public final String toString() {
    final CommandBuilder cb = new CommandBuilder(this);
    build(cb);
    return cb.toString();
  }

  /**
   * Checks if the specified filename is valid; allows only letters,
   * digits, the underscore, and dash.
   * @param name name to be checked
   * @return result of check
   */
  public static final boolean validName(final String name) {
    return name != null && name.matches("[\\w-]+");
  }

  // PROTECTED METHODS ========================================================

  /**
   * Executes the command and serializes the result.
   * @return success of operation
   * @throws IOException I/O exception
   */
  protected abstract boolean run() throws IOException;

  /**
   * Adds the error message to the message buffer {@link #info}.
   * @param msg error message
   * @param ext error extension
   * @return {@code false}
   */
  protected final boolean error(final String msg, final Object... ext) {
    info.reset();
    info.addExt(msg == null ? "" : msg, ext);
    return false;
  }

  /**
   * Adds information on command execution.
   * @param str information to be added
   * @param ext extended info
   * @return {@code true}
   */
  protected final boolean info(final String str, final Object... ext) {
    info.addExt(str, ext);
    info.add(NL);
    return true;
  }

  /**
   * Returns the specified command option.
   * @param typ options enumeration
   * @param <E> token type
   * @return option
   */
  protected final <E extends Enum<E>> E getOption(final Class<E> typ) {
    final E e = getOption(args[0], typ);
    if(e == null) error(CMDWHICH, args[0]);
    return e;
  }

  /**
   * Returns the specified command option.
   * @param s string to be found
   * @param typ options enumeration
   * @param <E> token type
   * @return option
   */
  protected static final <E extends Enum<E>> E getOption(final String s,
      final Class<E> typ) {
    try {
      return Enum.valueOf(typ, s.toUpperCase());
    } catch(final Exception ex) {
      return null;
    }
  }

  /**
   * Closes the specified database if it is currently opened and only
   * pinned once.
   * @param db database to be closed
   * @return closed flag
   */
  protected final boolean close(final String db) {
    final boolean close = context.data != null &&
    db.equals(context.data.meta.name) && context.datas.pins(db) == 1;
    if(close) new Close().run(context);
    return close;
  }

  // PRIVATE METHODS ==========================================================

  /**
   * Executes the command, prints the result to the specified output stream
   * and returns a success flag.
   * @param ctx database context
   * @param os output stream
   * @return success flag. The {@link #info()} method returns information
   * on a potential exception
   */
  private boolean exec(final Context ctx, final OutputStream os) {
    // check if data reference is available
    final Data data = ctx.data;
    if(data == null && (flags & DATAREF) != 0) return error(PROCNODB);

    // check permissions
    if(!ctx.perm(flags & 0xFF, data != null ? data.meta : null)) {
      final CmdPerm[] perms = CmdPerm.values();
      int i = perms.length;
      final int f = flags & 0xFF;
      while(--i >= 0 && (1 << i & f) == 0);
      return error(PERMNO, perms[i + 1]);
    }

    // check concurrency of commands
    boolean ok = false;
    final boolean writing = updating(ctx);
    ctx.lock.before(writing);
    ok = run(ctx, os);
    ctx.lock.after(writing);
    return ok;
  }

  /**
   * Runs the command without permission, data and concurrency checks.
   * @param ctx query context
   * @param os output stream
   * @return result of check
   */
  private boolean run(final Context ctx, final OutputStream os) {
    perf = new Performance();
    context = ctx;
    prop = ctx.prop;
    out = PrintOutput.get(os);

    try {
      return run();
    } catch(final ProgressException ex) {
      // process was interrupted by the user or server
      abort();
      return error(PROGERR);
    } catch(final Throwable ex) {
      // critical, unexpected error
      Performance.gc(2);
      Util.stack(ex);
      abort();
      if(ex instanceof OutOfMemoryError) return error(PROCMEM +
          ((flags & User.CREATE) != 0 ? PROCMEMCREATE : ""));

      final Object[] st = ex.getStackTrace();
      final Object[] obj = new Object[st.length + 1];
      obj[0] = ex.toString();
      System.arraycopy(st, 0, obj, 1, st.length);
      return error(Util.bug(obj));
    } finally {
      // flushes the output
      try { if(out != null) out.flush(); } catch(final IOException ex) { }
    }
  }
}
