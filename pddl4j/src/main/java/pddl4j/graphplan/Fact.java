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

import pddl4j.exp.AtomicFormula;
import pddl4j.exp.term.Term;

/**
 * This class implements the internal representation of a fact for the graphplan
 * planner.
 * 
 * @author Damien Pellier
 * @version 1.0
 */
public final class Fact extends AtomicFormula {

    /**
     * The serial version id of the class.
     */
    private static final long serialVersionUID = -4463978711591817598L;

    /**
     * The id of the fact.
     */
    private int id;

    /**
     * The first level where the fact appear in the planning graph.
     */
    private int level;
    
    
    /**
     * Create a new fact with a specified predicates and id.
     * 
     * @param p the predicate of the fact.
     * @param id the id of the fact.
     */
    public Fact(String p, int id) {
        super(p);
        this.id = id;
        this.level = -1;
    }
    
    /**
     * Create a new fact with a specified proposition and id.
     * 
     * @param prop the proposition of the fact.
     * @param id the id of the fact.
     */
    public Fact(AtomicFormula prop, int id) {
        super(prop.getPredicate());
        for (Term arg : prop) {
            super.add(arg);
        }
        this.id = id;
        this.level = -1;
    }
    
    /**
     * Returns the first level where the fact appear in the planning graph.
     * 
     * @return the first level where the fact appear in the planning graph or -1
     *         if the fact was yet in appeared.
     */
    public int getLevel() {
        return this.level;
    }

    /**
     * Sets the appearing level of the fact.
     *  
     * @param level the level.
     */
    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * Returns the id of the fact.
     * 
     * @return the id of the fact.
     */
    public int getId() {
        return this.id;
    }

    /**
     * Sets a new id to this fact.
     * 
     * @param id the new id to set.
     */
    public void setId(int id) {
        this.id = id;
    }
}
