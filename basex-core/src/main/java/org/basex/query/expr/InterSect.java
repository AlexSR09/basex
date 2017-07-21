package org.basex.query.expr;

import org.basex.query.*;
import org.basex.query.iter.*;
import org.basex.query.util.list.*;
import org.basex.query.value.item.*;
import org.basex.query.value.node.*;
import org.basex.query.var.*;
import org.basex.util.*;
import org.basex.util.hash.*;

/**
 * Intersect expression.
 *
 * @author BaseX Team 2005-17, BSD License
 * @author Christian Gruen
 */
public final class InterSect extends Set {
  /**
   * Constructor.
   * @param info input info
   * @param exprs expressions
   */
  public InterSect(final InputInfo info, final Expr[] exprs) {
    super(info, exprs);
  }

  @Override
  public Expr optimize(final CompileContext cc) throws QueryException {
    super.optimize(cc);
    return oneIsEmpty() ? cc.emptySeq(this) : this;
  }

  @Override
  protected ANodeList eval(final Iter[] iters, final QueryContext qc) throws QueryException {
    ANodeList list = new ANodeList();

    for(Item it; (it = iters[0].next()) != null;) {
      qc.checkStop();
      list.add(toNode(it));
    }
    final boolean db = list.dbnodes();

    final int el = exprs.length;
    for(int e = 1; e < el && !list.isEmpty(); ++e) {
      final ANodeList nt = new ANodeList().check();
      final Iter iter = iters[e];
      for(Item it; (it = iter.next()) != null;) {
        qc.checkStop();
        final ANode n = toNode(it);
        final int i = list.indexOf(n, db);
        if(i != -1) nt.add(n);
      }
      list = nt;
    }
    return list;
  }

  @Override
  public Expr copy(final CompileContext cc, final IntObjMap<Var> vm) {
    final InterSect is = new InterSect(info, copyAll(cc, vm, exprs));
    is.iterable = iterable;
    return copyType(is);
  }

  @Override
  protected NodeIter iter(final Iter[] iters) {
    return new SetIter(iters) {
      @Override
      public ANode next() throws QueryException {
        final int irl = iter.length;
        if(item == null) item = new ANode[irl];

        for(int i = 0; i < irl; i++) if(!next(i)) return null;

        final int il = item.length;
        for(int i = 1; i < il;) {
          final int d = item[0].diff(item[i]);
          if(d > 0) {
            if(!next(i)) return null;
          } else if(d < 0) {
            if(!next(0)) return null;
            i = 1;
          } else {
            ++i;
          }
        }
        return item[0];
      }
    };
  }
}
