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

import java.util.BitSet;

import pddl4j.exp.AtomicFormula;

/**
 * This class implements the internal representation of an operator for the
 * graphplan planner.
 * 
 * @author Damien Pellier
 * @version 1.0
 */
public final class Op {

    /**
     * The name of the operator.
     */
    private AtomicFormula name;

    /**
     * The positive precondition of the operator.
     */
    private BitSet pp;

    /**
     * The negative precondition of the operator.
     */
    private BitSet np;

    /**
     * The positive effect of the operator.
     */
    private BitSet pe;

    /**
     * The negative effect of the operator.
     */
    private BitSet ne;

    /**
     * The id of the operator.
     */
    private int id;

    /**
     * The level of the planning graph where the operator appear for the first
     * time.
     */
    private int level;

    /**
     * Creates a new operator.
     * 
     * @param name the name of the operator.
     * @param pp the positive precondition of the operator.
     * @param np the negative precondition of the operator.
     * @param pe the positive effect of the operator.
     * @param ne the negative effect of the operator.
     * @param id the id of the operator.
     */
    public Op(AtomicFormula name, BitSet pp, BitSet np, BitSet pe, BitSet ne,
                int id) {
        this.name = name;
        this.pp = pp;
        this.np = np;
        this.pe = pe;
        this.ne = ne;
        this.id = id;
        this.level = -1;
    }

    /**
     * Returns the level of the operator.
     * 
     * @return the level of the operator.
     */
    public int getLevel() {
        return level;
    }

    /**
     * Sets a new level to this operator.
     * 
     * @param level the level to set
     */
    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * Returns the id of the operator.
     * 
     * @return the id.
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the positive effect of this operator.
     * 
     * @return the positive effect of this operator.
     */
    public BitSet getPositiveEffect() {
        return this.pe;
    }

    /**
     * Returns the negative effect of this operator.
     * 
     * @return the the negative effect of this operator.
     */
    public BitSet getNegativeEffect() {
        return this.ne;
    }

    /**
     * Returns the negative precondition of this operator.
     * 
     * @return the negative precondition of this operator.
     */
    public BitSet getNegativePrecondition() {
        return this.np;
    }

    /**
     * Returns the positive precondition of this operator.
     * 
     * @return the positive precondition of this operator.
     */
    public BitSet getPositivePreconditon() {
        return this.pp;
    }

    /**
     * Return the name of this operator.
     * 
     * @return the name of this operator
     */
    public AtomicFormula getName() {
        return this.name;
    }

    /**
     * Returns <code>true</code> if this operator is equal to an object. This
     * method returns <code>true</code> if the object is a not null instance
     * of the class <code>Op</code> and both operator have the same name.
     * 
     * @param obj the object to be compared.
     * @return <code>true</code> if this operator is equal to an object;
     *         <code>false</code> otherwise.
     */
    public boolean equals(Object obj) {
        if (obj != null && this.getClass().equals(obj.getClass())) {
            Op other = (Op) obj;
            return this.name.equals(other.name);
        }
        return false;
    }
    
    /**
     * Returns a hash code value for this operator. This method is supported for the
     * benefit of hashtables such as those provided by
     * <code>java.util.Hashtable</code>.
     * 
     * @return a hash code value for this operator.
     */
    public int hashCode() {
        return this.name.hashCode();
    }

}
