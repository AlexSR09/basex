package org.basex.query.func.validate;

import org.basex.query.*;
import org.basex.query.func.*;
import org.basex.query.value.item.*;
import org.basex.util.*;

/**
 * Function implementation.
 *
 * @author BaseX Team 2005-19, BSD License
 * @author Christian Gruen
 */
public final class ValidateXsdProcessor extends StandardFunc {
  @Override
  public Item item(final QueryContext qc, final InputInfo ii) {
    return Str.get(ValidateXsd.IMPL[ValidateXsd.OFFSET + 1]);
  }
}
