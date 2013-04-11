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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import pddl4j.exp.AtomicFormula;
import pddl4j.exp.term.Term;

/**
 * This class implements a layered plan.
 * 
 * @author Damien Pellier
 * @version 1.0 
 */
public final class Plan implements Iterable<Set<AtomicFormula>> {
    
    /**
     * The constant failure plan.
     */
    public static final Plan FAILURE = null;
    /**
     * The constant empty plan.
     */
    public static final Plan EMPTY = new Plan();
    
    /**
     * The size of the plan, i.e., the number of actions contains in the plan.
     */
    private int size;
    
    /**
     * The list used to store the actions of the plan.
     */
    private ArrayList<Set<AtomicFormula>> actions;
    
    /**
     * Creates a new empty plan.
     */
    public Plan() {
        this.actions = new ArrayList<Set<AtomicFormula>>();
        this.size = 0;
    }
    
    /**
     * Adds a new action in the k layer of the plan.
     * 
     * @param op_name The action to add.
     * @param k the layer where the action must be added.
     * @return <code>true</code> if the actions was added; <code>false</code>
     *         otherwise.
     */
    public boolean add(AtomicFormula op_name, int k) {
        int layers = this.actions.size();
        if (k >= layers) {
            int addLayers = k - layers + 1;
            for (int i = 0; i < addLayers; i++) {
                this.actions.add(new LinkedHashSet<AtomicFormula>());
            }
        }
        Set<AtomicFormula> ak = this.actions.get(k);
        boolean added = ak.add(op_name);
        if (added) this.size++;
        return added;
    }
        
    /**
     * Returns the set of actions contains in a specified layer.
     * 
     * @param k the layer.
     * @return the set of actions contains in a specified layer.
     */
    public Set<AtomicFormula> get(int k) {
        return this.actions.get(k);
    }
    
    /**
     * Returns the number of layers of the plan.
     * 
     * @return the number of layers of the plan.
     */
    public int layers() {
        return this.actions.size();
    }
    
    /**
     * Returns the number of actions contains in the plan.
     * 
     * @return the number of actions contains in the plan.
     */
    public int size() {
        return this.size;
    }
    
    /**
     * Returns an iterator over the layers of the plan.
     * 
     * @return an iterator over the layers of the plan.
     */
    public Iterator<Set<AtomicFormula>> iterator() {
        return this.actions.iterator();
    }
    
    /**
     * Returns <code>true</code> if this plan is equals to an other object.
     * This method returns <code>true</code> if the object is a not null
     * instance of the class <code>Plan</code> and both plan have the same
     * layers.
     * 
     * @param obj the object to be compared.
     * @return <code>true</code> if this plan is equals to an other object;
     *         <code>false</code> otherwise.
     */
    public boolean equals(Object obj) {
        if (obj != null && this.getClass().equals(obj.getClass())) {
            Plan other = (Plan) obj;
            return this.actions.equals(other.actions);
        }
        return false;
    }

    /**
     * Returns a hash code value for this plan. This method is supported for the
     * benefit of hashtables such as those provided by
     * <code>java.util.Hashtable</code>.
     * 
     * @return a hash code value for this plan.
     */
    public int hashCode() {
        return this.actions.hashCode();
    }
    
    /**
     * Returns a string representation of this plan.
     * 
     * @return a string representation of this plan.
     */
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        int i = 0;
        for (Set<AtomicFormula> layer : this) {
            StringBuffer str = new StringBuffer();
            buffer.append("time step " + i + ":");
            int j = 0;
            for (AtomicFormula action : layer) {
                StringBuffer as = new StringBuffer();
                as.append(action.getPredicate().toUpperCase());
                for (Term parameter : action) {
                    as.append(" " + parameter.getImage().toUpperCase());
                }
                if (j > 0) str.append("                ");
                str.append(as.toString());
                if (j < layer.size() - 1) str.append("\n");
                j++;
            }
            buffer.append(str.toString());
            i++;
        }
       return buffer.toString(); 
    }
}
