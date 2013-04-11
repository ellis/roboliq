/*
 * Copyright Dept. of Mathematics & Computer Science Univ. Paris-Descartes
 *
 * This software is governed by the CeCILL  license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */

package pddl4j.graphplan;

import java.io.Serializable;
import java.util.Arrays;
import java.util.BitSet;

/**
 * This class implements a bit matrix.
 * 
 * @author Damien Pellier
 * @version 1.0 
 */
final class BitMatrix implements Cloneable, Serializable {
    
    /**
     * The serial version id of the class.
     */
    private static final long serialVersionUID = 2831903376066135583L;
    
    /**
     * The number of rows.
     */
    private int rows;
    
    /**
     * The number of columns.
     */
    private int columns;
    
    /**
     * The array of bit set used to to store the matrix.
     */
    private BitSet[] bitsets;
    
    /**
     * Creates a new bit matrix with a specified number of rows and columns.
     * 
     * @param rows The number of rows of the matrix.
     * @param columns The number of column of the matrix.
     */
    public BitMatrix(final int rows, final int columns) {
        this.rows = rows;
        this.columns = columns;
        this.bitsets = new BitSet[this.rows];
        for (int i = 0; i < this.rows; i++) {
            this.bitsets[i] = new BitSet(this.columns);
        }
    }
    
    /**
     * Creates a new squared matrix of a specific size.
     * 
     * @param size the size of the squared matrix.
     */
    public BitMatrix(int size) {
        this(size, size);
    }
    
    /**
     * Sets the bit at a specified row and column position to true.
     * 
     * @param i the row position.
     * @param j the column position.
     */
    public void set(int i, int j) {
        this.bitsets[i].set(j);
    }
    
    /**
     * Sets the bit at a specified row and column position to false.
     * 
     * @param i the row position.
     * @param j the column position.
     */
    public void clear(int i, int j) {
        this.bitsets[i].clear(j);
    }
    
    /**
     * Returns the ith row of the matrix.
     * 
     * @param i the index of the row.
     * @return the ith row of the matrix.
     */
    public BitSet getRow(int i) {
        return this.bitsets[i];
    }
    
    /**
     * Returns the jth column of the matrix.
     * 
     * @param j the index of the column.
     * @return the jth column of the matrix.
     */
    public BitSet getColumn(int j) {
        BitSet column = new BitSet(this.rows);
        for (int i = 0; i < this.rows; i++) {
            column.set(i, this.bitsets[i].get(j));
        }
        return column;
    }
    
    /**
     * Returns the value of the bit at a specific position in the matrix.
     * 
     * @param i The row of the bit.
     * @param j The column of the bit.
     * @return the value of the bit at a specific position in the matrix.
     */
    public boolean get(int i, int j) {
        return this.bitsets[i].get(j);
    }
    
    /**
     * Returns the cardinality of the matrix, i.e., the number of bits set to 1
     * in the matrix.
     * 
     * @return Returns the cardinality of the matrix.
     */
    public int cardinality() {
        int cardinality = 0;
        for (int i = 0; i < this.rows; i++) {
            cardinality += this.bitsets[i].cardinality();
        }
        return cardinality;
    }
    
    /**
     * Returns the number of columns of the matrix.
     * 
     * @return the number of columns of the matrix.
     */
    public int columns() {
        return this.columns;
    }
    
    /**
     * Returns the number of rows of the matrix.
     * 
     * @return the number of rows of the matrix.
     */
    public int rows() {
        return this.rows;
    }
    
    /**
     * Returns a copy of the matrix. 
     * 
     * @return a copy of the matrix.
     * @see java.lang.Object#clone()
     */
    public Object clone() {
        try {
            final BitMatrix clone = (BitMatrix) super.clone();
            System.arraycopy(this.bitsets, 0, clone.bitsets, 0, this.bitsets.length);
            return clone;
        } catch (CloneNotSupportedException e) {
          throw new InternalError();
        }
    }
    
    /**
     * Returns <code>true</code> if this matrix is equals to an other object.
     * 
     * @param obj the object to compared.
     * @return <code>true</code> if this matrix is equals to an other object;
     *         <code>false</code> otherwise.
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof BitMatrix) {
            BitMatrix other = (BitMatrix) obj;
            return Arrays.equals(this.bitsets, other.bitsets);
        }
        return false;
    }
    
    /**
     * Returns the hash code value of this matrix.
     * 
     * @return the hash code value of this matrix.
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return Arrays.hashCode(this.bitsets);
    }
    
    /**
     * Returns a string representation of the matrix.
     * 
     * @return a string representation of the matrix.
     */
    public String toString() {
        return Arrays.toString(this.bitsets);
    }
}
