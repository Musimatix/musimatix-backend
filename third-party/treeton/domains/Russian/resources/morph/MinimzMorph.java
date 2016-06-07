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

import treeton.res.minimz.TypeMatrixTriplet;
import treeton.res.minimz.TypeMatrix;
import treeton.res.minimz.elector.ElectorEnddot;

import java.util.Iterator;
import java.util.ArrayList;

public class MinimzMorph implements Iterable<TypeMatrixTriplet> {
  ArrayList<TypeMatrixTriplet> a = new ArrayList<TypeMatrixTriplet>();

  public MinimzMorph() {
    TypeMatrix matrix = new TypeMatrix(new String[] {"Gramm"},new ElectorEnddot());
    a.add(new TypeMatrixTriplet(null,null,matrix));
  }

  public Iterator<TypeMatrixTriplet> iterator() {
    return a.iterator();
  }
}