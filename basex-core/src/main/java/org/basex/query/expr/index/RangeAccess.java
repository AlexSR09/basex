package org.basex.query.expr.index;

import static org.basex.query.QueryText.*;

import org.basex.data.*;
import org.basex.index.*;
import org.basex.index.query.*;
import org.basex.query.*;
import org.basex.query.expr.*;
import org.basex.query.func.*;
import org.basex.query.iter.*;
import org.basex.query.value.*;
import org.basex.query.value.item.*;
import org.basex.query.value.node.*;
import org.basex.query.value.seq.*;
import org.basex.query.value.type.*;
import org.basex.query.var.*;
import org.basex.util.*;
import org.basex.util.hash.*;
import org.basex.util.list.*;

/**
 * This index class retrieves numeric ranges from a value index.
 *
 * @author BaseX Team 2005-19, BSD License
 * @author Christian Gruen
 */
public final class RangeAccess extends IndexAccess {
  /** Index token. */
  private final NumericRange index;

  /**
   * Constructor.
   * @param info input info
   * @param index index token
   * @param db index database
   */
  public RangeAccess(final InputInfo info, final NumericRange index, final IndexDb db) {
    super(db, info, index.type());
    this.index = index;
  }

  @Override
  public BasicNodeIter iter(final QueryContext qc) throws QueryException {
    final IndexType type = index.type();
    final Data data = db.data(qc, type);

    return new DBNodeIter(data) {
      final byte kind = type == IndexType.TEXT ? Data.TEXT : Data.ATTR;
      final IndexIterator ii = data.iter(index);

      @Override
      public DBNode next() {
        return ii.more() ? new DBNode(data, ii.pre(), kind) : null;
      }
    };
  }

  @Override
  public Value value(final QueryContext qc) throws QueryException {
    final IndexType it = index.type();
    final Data data = db.data(qc, it);

    final NodeType type = it == IndexType.TEXT ? NodeType.TXT : NodeType.ATT;
    final IndexIterator ii = data.iter(index);

    final IntList list = new IntList();
    while(ii.more()) list.add(ii.pre());
    return DBNodeSeq.get(list.finish(), data, type, false);
  }

  @Override
  public Expr copy(final CompileContext cc, final IntObjMap<Var> vm) {
    return new RangeAccess(info, index, db.copy(cc, vm));
  }

  @Override
  public boolean equals(final Object obj) {
    return obj instanceof RangeAccess && index.equals(((RangeAccess) obj).index) &&
        super.equals(obj);
  }

  @Override
  public void plan(final QueryPlan plan) {
    plan.add(plan.create(this, INDEX, index.type(), MIN, index.min, MAX, index.max), db);
  }

  @Override
  public String toString() {
    final Function func = index.type() == IndexType.TEXT ? Function._DB_TEXT_RANGE :
      Function._DB_ATTRIBUTE_RANGE;
    return func.args(db.source(), Dbl.get(index.min), Dbl.get(index.max)).substring(1);
  }
}
