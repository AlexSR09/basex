package org.basex.query.xquery.expr;

import org.basex.query.xquery.XQException;
import org.basex.query.xquery.XQContext;
import org.basex.query.xquery.item.FTNodeItem;
import org.basex.query.xquery.iter.FTNodeIter;
import org.basex.query.xquery.iter.Iter;
import org.basex.util.IntList;

/**
 * FTUnion expression.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-08, ISC License
 * @author Christian Gruen
 */
public final class FTUnion extends FTExpr {
  /** Cache for one of the nodes. */
  private IntList cp;
  /** Flag is set, if ith expression has any result. */
  private FTNodeItem[] mp;
  /** Pointer on the positive expression with the lowest pre-values.*/
  private int minp = -1;
  /** Saving index of positive expressions. */
  private int[] pex;
  /** Flag if one result was a ftnot. */
  private boolean not = false;
  
  /**
   * Constructor.
   * @param posex pointer on expression
   * @param ftnot flag for ftnot expression
   * @param e expression list
   * 
   */
  public FTUnion(final int[] posex, final boolean ftnot, final FTExpr... e) {
    super(e);
    pex = posex;
    mp = new FTNodeItem[pex.length];
    cp = new IntList(pex);
    not = ftnot;
  }

  @Override
  public Iter iter(final XQContext ctx) {
    return new FTNodeIter(){
      @Override
      public FTNodeItem next() throws XQException { 
        if (FTUnion.this.more(ctx)) 
        return FTUnion.this.next();
        else 
          return FTUnion.this.next();
        }
    };
  }
  
  /**
   * Checks if more results exist.
   * @param ctx current context
   * @return boolean
   * @throws XQException Exception
   */
  public boolean more(final XQContext ctx) throws XQException {
    boolean b = false;
    if (cp.size > 0) {
      for (int i = 0; i < cp.size; i++) {
        mp[pex[cp.list[i]]] = (FTNodeItem) 
        ctx.iter(expr[pex[cp.list[i]]]).next();
        if (!b) b = mp[i].ftn.size > 0;
      }
      cp.reset();
    }
    if (!b) {
      for (FTNodeItem c : mp) if(c.ftn.size > 0) return true;
    }
    return b;
  }
  
  /**
   * Get next result.
   * 
   * @return next result
   */
  public FTNodeItem next() {
    if (minp == -1) {
      minp = 0;
      while(minp < mp.length && mp[minp].ftn.size == 0) minp++;
      if (minp < mp.length) cp.set(minp, 0);
      for (int ip = minp + 1; ip < pex.length; ip++) {       
        if (mp[ip].ftn.size > 0) { 
          final FTNodeItem n1 = mp[pex[ip]];
          final FTNodeItem n2 = mp[pex[minp]];
          if (n1.ftn.getPre() < n2.ftn.getPre()) {
            minp = ip;
            cp.set(ip, 0);
          } else if (n1.ftn.getPre() == n2.ftn.getPre()) {
              cp.add(ip);
          }
        } 
      }
    }
    
    minp = -1;
    final FTNodeItem m = mp[pex[cp.list[0]]];
    for (int i = 1; i < cp.size; i++) {
      m.merge(mp[pex[cp.list[i]]], 0);
      // in case of ftor !"a" ftor "b" "a b" is result
      m.ftn.not = false;
    }
    
    // ftnot causes to set this flag (seq. index mode)
    if (m.ftn.size == 0) m.ftn.not = not;
    return m;
  }

  @Override
  public String toString() {
    return toString(" ftunion ");
  }
  
  }
