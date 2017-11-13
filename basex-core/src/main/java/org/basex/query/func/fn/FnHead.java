package org.basex.query.func.fn;

import org.basex.query.*;
import org.basex.query.expr.*;
import org.basex.query.func.*;
import org.basex.query.value.item.*;
import org.basex.query.value.type.SeqType.*;
import org.basex.util.*;

/**
 * Function implementation.
 *
 * @author BaseX Team 2005-17, BSD License
 * @author Christian Gruen
 */
public final class FnHead extends StandardFunc {
  @Override
  public Item item(final QueryContext qc, final InputInfo ii) throws QueryException {
    final Expr ex = exprs[0];
    return ex.seqType().zeroOrOne() ? ex.item(qc, info) : qc.iter(ex).next();
  }

  @Override
  protected Expr opt(final CompileContext cc) {
    final Expr ex = exprs[0];
    if(ex.seqType().zeroOrOne()) return ex;
    exprType.assign(Occ.ZERO_ONE);
    return this;
  }
}
