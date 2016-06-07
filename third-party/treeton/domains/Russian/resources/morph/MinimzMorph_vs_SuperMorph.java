/*
 * $:$:$ $:$:$[
 *
 * Copyright © Ontos AG
 *
 * Mittelstrasse 24
 * CH-2560 Nidau
 * Telefon +41 (0) 32 332 82 70
 * Fax +41 (0) 32 332 92 52
 * sales@ontos.ch
 *
 * ]$:$:$ $:$:$
 */
package treeton.res.minimz;

import treeton.res.minimz.*;
import treeton.res.minimz.elector.*;

import java.util.Iterator;
import java.util.ArrayList;

public class MinimzMorph_vs_SuperMorph implements Iterable<TypeMatrixTriplet> {
  ArrayList<TypeMatrixTriplet> arr = new ArrayList<TypeMatrixTriplet>();

  public MinimzMorph_vs_SuperMorph() {
    TypePriority[] pr;

    pr = new TypePriority[] {
      new TypePriority("Gramm","SuperMorph",new ElectorFirstGramm(
        new String[] {
          "base", "POS", "REPR", "VOX", "MD",   "TNS",  "ASP", "TRANS",
          "PRS", "CAS",  "NMB", "ANIM", "GEND", "ATTR",
          "TYPE",   "PNT"
        }
      )),
    };
    TypeMatrix matrix = new TypeMatrix(pr, TypeMatrix.SYM_INV);
    arr.add(new TypeMatrixTriplet(matrix,null,null));
  }

  public Iterator<TypeMatrixTriplet> iterator() {
    return arr.iterator();
  }
}