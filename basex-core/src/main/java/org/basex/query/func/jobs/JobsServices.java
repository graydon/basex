package org.basex.query.func.jobs;

import static org.basex.query.QueryError.*;

import java.io.*;

import org.basex.core.jobs.*;
import org.basex.query.*;
import org.basex.query.func.*;
import org.basex.query.iter.*;

/**
 * Function implementation.
 *
 * @author BaseX Team 2005-18, BSD License
 * @author Christian Gruen
 */
public final class JobsServices extends StandardFunc {
  @Override
  public Iter iter(final QueryContext qc) throws QueryException {
    checkAdmin(qc);
    try {
      return new Jobs(qc.context).toXML().children();
    } catch(final IOException ex) {
      throw JOBS_SERVICE_X_X.get(info, ex);
    }
  }
}
