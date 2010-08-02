package org.basex.query.expr;

import static org.basex.query.QueryTokens.*;
import static org.basex.query.QueryText.*;
import java.io.IOException;
import org.basex.data.Serializer;
import org.basex.query.QueryContext;
import org.basex.query.QueryException;
import org.basex.query.item.Item;
import org.basex.query.item.Seq;
import org.basex.query.item.SeqType;
import org.basex.util.InputInfo;
import org.basex.util.Token;

/**
 * Calculation.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
 * @author Christian Gruen
 */
public final class Clc extends Arr {
  /** Calculation operator. */
  private final Calc calc;

  /**
   * Constructor.
   * @param ii input info
   * @param e1 first expression
   * @param e2 second expression
   * @param c calculation operator
   */
  public Clc(final InputInfo ii, final Expr e1, final Expr e2, final Calc c) {
    super(ii, e1, e2);
    calc = c;
  }

  @Override
  public Expr comp(final QueryContext ctx) throws QueryException {
    super.comp(ctx);

    final Expr e = expr[0].item() && expr[1].item() ? atomic(ctx, input) :
      expr[0].empty() || expr[1].empty() ? Seq.EMPTY : this;

    if(e != this) ctx.compInfo(OPTPRE, this);
    return e;
  }

  @Override
  public Item atomic(final QueryContext ctx, final InputInfo ii)
      throws QueryException {
    final Item a = expr[0].atomic(ctx, input);
    if(a == null) return null;
    final Item b = expr[1].atomic(ctx, input);
    if(b == null) return null;
    return calc.ev(input, a, b);
  }

  @Override
  public void plan(final Serializer ser) throws IOException {
    ser.openElement(this, TYPE, Token.token(calc.name));
    for(final Expr e : expr) e.plan(ser);
    ser.closeElement();
  }

  @Override
  public SeqType returned(final QueryContext ctx) {
    final SeqType s0 = expr[0].returned(ctx);
    final SeqType s1 = expr[1].returned(ctx);
    return s0.num() && s1.num() ? SeqType.ITR :
      s0.one() && s1.one() ? SeqType.ITEM : SeqType.ITEM_ZO;
  }

  @Override
  public String desc() {
    return "'" + calc.name + "' expression";
  }

  @Override
  public String color() {
    return "FF9966";
  }

  @Override
  public String toString() {
    return toString(' ' + calc.name + ' ');
  }
}
