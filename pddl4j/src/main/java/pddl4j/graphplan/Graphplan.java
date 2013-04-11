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

import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;

import pddl4j.ErrorManager;
import pddl4j.PDDLObject;
import pddl4j.Parser;
import pddl4j.RequireKey;
import pddl4j.Source;
import pddl4j.ErrorManager.Message;
import pddl4j.InvalidExpException;
import pddl4j.exp.AndExp;
import pddl4j.exp.AtomicFormula;
import pddl4j.exp.Exp;
import pddl4j.exp.ExpID;
import pddl4j.exp.InitEl;
import pddl4j.exp.Literal;
import pddl4j.exp.NotAtomicFormula;
import pddl4j.exp.NotExp;
import pddl4j.exp.term.Substitution;
import pddl4j.exp.term.Variable;
import pddl4j.exp.term.Term;
import pddl4j.exp.action.Action;
import pddl4j.exp.action.ActionDef;
import pddl4j.exp.action.ActionID;
import pddl4j.exp.fcomp.FCompExp;
import pddl4j.exp.term.Constant;

/**
 * This class implements the graphplan planner as an example of the pddl4j
 * library. Only strips and typing are accepted.
 * 
 * @author Damien Pellier
 * @version 1.0
 */
public final class Graphplan {

    /**
     * The problem to solve.
     */
    private PDDLObject problem;
    
    /**
     * The bit set that represents the goal of the problem.
     */
    private BitSet goals;
    
    /**
     * The list of operators layers of the planning graph. 
     */
    private ArrayList<BitSet> ops_layers;
    
    /**
     * The list of facts layers of the planning graph. 
     */
    private ArrayList<BitSet> facts_layers;
   
    /**
     * The list of operators mutex of the planning graph. 
     */
    private ArrayList<BitMatrix> ops_mutex;
    
    /**
     * The list of facts mutex of the planning graph. 
     */
    private ArrayList<BitMatrix> facts_mutex;
    
    /**
     * The list of operators of the planning problem.
     */
    private ArrayList<Op> ops_table = new ArrayList<Op>(1000);
    
    /**
     * The list of facts of the planning problem.
     */
    private ArrayList<Fact> facts_table;
    
    /**
     * The matrix used to store the static dependences between the operators. 
     */
    private BitMatrix dependences;
    
    /**
     * The fixed point of the planning graph. 
     */
    private int levelOff = -1;
    
    /**
     * The index of the first operator in the operator table which is not a noops.
     */
    private int ops_index;
    
    /**
     * The table used to memorize the goals failure during search.
     */
    private HashMap<Integer, Set<BitSet>> nogoods;
    
    /**
     * The list of operators resolvers of the planning graph.
     */
    private ArrayList<BitMatrix> resolvers;
    
    /**
     * The size of the facts table.
     */
    private int facts_table_size;
    
    /**
     * The size of the operators table.
     */
    private int ops_table_size;
    
    /**
     * The time spent for building graph.
     */
    private long g_time = 0;
    
    /**
     * The time spent for searching a solution plan.
     */
    private long s_time = 0;
    
    /**
     * The time spent for preprocessing the planning problem.
     */
    private long p_time = 0;
    
    /**
     * The time spent for computing the mutual exclusions.
     */
    private long m_time = 0;
    
    /**
     * The number of actions tried for finding a solution plan.
     */
    private long a_tried = 0;
    
    /**
     * The number of noop actions tried for finding a solution plan.
     */
    private long noop_tried = 0;

    /**
     * Creates a new java graphplan planner to solve a specific problem.
     * 
     * @param problem the problem to solve.
     */
    private Graphplan(PDDLObject problem) {
        this.problem = problem;
        this.facts_layers = new ArrayList<BitSet>(100);
        this.ops_layers = new ArrayList<BitSet>(100);
        this.facts_mutex = new ArrayList<BitMatrix>(100);
        this.ops_mutex = new ArrayList<BitMatrix>(100);
        this.resolvers = new ArrayList<BitMatrix>(100);
    }
    

    /**
     * The main method the jgraphplan planner.
     * 
     * @param args an array of string representing the path of the planning
     *            domain and the problem.
     */
    public static void main(String[] args) {
        try {
            // Checks the command line
            if (args.length != 2) {
                Graphplan.printUsage();
            } else {
                // Gets the pddl compiler options
                Properties options = Graphplan.getParserOptions();
                // Creates an instance of the java pddl compiler
                if (!new File(args[0]).exists()) {
                    System.out.println("domain file " + args[0] + " does not exist");
                    System.exit(0);
                }
                if (!new File(args[1]).exists()) {
                    System.out.println("problem file " + args[0] + " does not exist");
                    System.exit(0);
                }
                Parser parser = new Parser(options);
                PDDLObject domain = parser.parse(new File(args[0]));
                PDDLObject problem = parser.parse(new File(args[1]));
                PDDLObject pb = null;
                if (domain != null && problem != null) {
                    pb = parser.link(domain, problem);
                }
                // Gets the error manager of the pddl compiler
                ErrorManager mgr = parser.getErrorManager();
                // If the compilation produces errors we print it and stop
                if (mgr.contains(Message.ERROR)) {
                    mgr.print(Message.ALL);
                }
                // else we print the warning and start the planning process
                else {
                    
                    mgr.print(Message.WARNING);
                    
                    System.out.println("\nParsing domain \"" + domain.getDomainName() + "\" done successfully ...");
                    System.out.println("Parsing problem \"" + problem.getProblemName() + "\" done successfully ...\n");
                    
                    Graphplan planner = new Graphplan(pb);
                    planner.preprocessing();
                    Plan plan = planner.solve();
                    
                    if (plan != Plan.FAILURE) {
                        System.out.println("\nfound plan as follows:\n");
                        Graphplan.printPlan(plan);
                    } else {
                        System.out.println("\nno solution plan found\n");
                    }
                        System.out.printf("\nnumber of actions tried : %12d \n", planner.a_tried);
                        System.out.printf("number of noops tried   : %12d \n\n", planner.noop_tried);
                    
                    final double p_time = planner.p_time/1000000000.0;
                    final double g_time = planner.g_time/1000000000.0;
                    final double s_time = planner.s_time/1000000000.0;
                    final double m_time = planner.m_time/1000000000.0;
                    final double t_time = p_time + g_time + s_time;
                    System.out.printf("Time spent : %8.2f seconds preprocessing \n", p_time);
                    System.out.printf("             %8.2f seconds build graph \n", g_time);
                    System.out.printf("             %8.2f seconds calculating exclusions \n", m_time);
                    System.out.printf("             %8.2f seconds searching graph \n", s_time);
                    System.out.printf("             %8.2f seconds total time \n", t_time);
                }
            }
        } catch (Throwable t) {
            System.err.println(t.getMessage());
            t.printStackTrace(System.err);
        }
    }
    
    
    /**
     * Returns the default options of the compiler.
     * 
     * @return the default options.
     */
    private static Properties getParserOptions() {
        Properties options = new Properties();
        options.put("source", Source.V3_0);
        options.put(RequireKey.STRIPS, true);
        options.put(RequireKey.TYPING, true);
        options.put(RequireKey.EQUALITY, true);
        options.put(RequireKey.NEGATIVE_PRECONDITIONS, false);
        options.put(RequireKey.DISJUNCTIVE_PRECONDITIONS, true);
        options.put(RequireKey.EXISTENTIAL_PRECONDITIONS, true);
        options.put(RequireKey.UNIVERSAL_PRECONDITIONS, true);
        options.put(RequireKey.CONDITIONAL_EFFECTS, true);
        return options;
    }
   
    
    /**
     * Print the usage of the graphplan command.
     */
    private static void printUsage() {
        System.out.println("Usage: graphplan [domain] [problem]");
        System.exit(0);   
    } 
    
    //-----------------------------------------------------------------------//
    // Methods used for search a plan                                        //
    //-----------------------------------------------------------------------//   

    /**
     * Solves the planning problem and returns the first solution plan found.
     * 
     * @return the first solution plan found or <code>Plan.Failure</code> if
     *         no solution was found.
     */
    public Plan solve() {
        Plan plan = Plan.FAILURE;
        int nbNoGoods = 0;
        // Initial expansion of the planning graph
        int k = 0;
        while ((!this.contains(this.goals, k) || !this.isMutexFree(this.goals, k)) && !this.isLevelOff()) {
            this.expand(k);
            k++;
            if (this.contains(this.goals, k) && this.isMutexFree(this.goals, k)) {
                System.out.print("\ngoals first reachable in " + k + " times steps\n\n");
            }            
        } 
        
        if (!this.contains(this.goals, k) || !this.isMutexFree(goals, k)) {
            return Plan.FAILURE;
        }
        
        long begin = System.nanoTime();
        plan = this.extract(goals, k);
        long end = System.nanoTime();
        this.s_time += end -begin;
        
        if (this.isLevelOff()) {
            nbNoGoods = nogoods.get(this.levelOff).size();
        } else {
            nbNoGoods = 0;
        }
        
        while (plan == Plan.FAILURE) {
            this.expand(k);    
            k++;
            begin = System.nanoTime();
            plan = this.extract(goals, k);
            end = System.nanoTime();
            this.s_time += end - begin;     
            if (plan == Plan.FAILURE && this.isLevelOff()) {
                final int nbNoGoodsAtLevelOff = nogoods.get(this.levelOff).size();
                if (nbNoGoods == nbNoGoodsAtLevelOff) {
                    plan = Plan.FAILURE;
                } else {
                    nbNoGoods = nbNoGoodsAtLevelOff;
                }
            }
        }   
        return plan;
    }
    
    /**
     * Extract the solution plan from the planning graph.
     * 
     * @param goals the goals to be extracted.
     * @param k the level of the graph where the goal must be extracted.
     * 
     * @return the solution plan extract at level k.
     */
    private Plan extract(final BitSet goals, final int k) {
        Plan plan = null;
        if (k == 0) {
            return plan = Plan.EMPTY;
        }
        Set<BitSet> ngk = nogoods.get(k);
        if (ngk != null && ngk.contains(goals)) {
            return Plan.FAILURE;
        }
        plan = this.search(goals, new BitSet(this.ops_table_size), k, new BitSet(this.facts_table_size));
        if (plan == Plan.FAILURE) { 
            ngk = nogoods.get(k);
            if (ngk == null) {
                ngk = new HashSet<BitSet>(); 
                nogoods.put(k, ngk);
            }
            ngk.add(goals);
        }
        return plan;
    }

    
    /**
     * Search the actions that resolve a list of goals at a speific level. 
     * 
     * @param goals The list of goals to resolve.
     * @param ak The set of actions that already solve the goals.
     * @param k The level of the planning graphh where the search is done.
     * @param mutex the set of mutex already computed.
     * @return The plan that solve the list of goals or null if no plan can solve it.
     */
    private Plan search(final BitSet goals, final BitSet ak, final int k, final BitSet mutex) {
        Plan plan = Plan.FAILURE;
        if (goals.isEmpty()) {
            for (int i = ak.nextSetBit(0); i >= 0; i = ak.nextSetBit(i + 1)) {
                goals.or(this.ops_table.get(i).getPositivePreconditon());
            }
            plan = this.extract(goals, k - 1);
            if (plan !=  Plan.FAILURE) {
                for (int i = ak.nextSetBit(0); i >= 0; i = ak.nextSetBit(i + 1)) {
                    AtomicFormula op_name = this.ops_table.get(i).getName();
                    if (!op_name.getPredicate().startsWith("noop")) {
                        plan.add(op_name, k - 1);
                    }
                }
                
            }
        } else {
            
            final Fact goal = this.selectGoal(goals);
            final BitSet resolvers = (BitSet) this.getResolvers(goal.getId(), k).clone();
            resolvers.andNot(mutex);
            
            while (!resolvers.isEmpty() && plan == null) {
                final Op resolver = this.selectResolver(resolvers);
                final int resolver_id = resolver.getId();
                resolvers.clear(resolver_id);
                final BitSet newAk = (BitSet) ak.clone();
                newAk.set(resolver_id);
                final BitSet newMutex = (BitSet) mutex.clone();
                newMutex.or(this.ops_mutex.get(k - 1).getRow(resolver_id));
                final BitSet newGoals = (BitSet)  goals.clone();
                newGoals.andNot(resolver.getPositiveEffect());
                plan = this.search(newGoals, newAk, k, newMutex);    
            }
        }
        return plan;

    }
   
    /**
     * Select a goal from a set of goals. The goals selection is based on the
     * level heuristics, i.e., choose always the goal fact that appears earliest
     * in the planning graph because it is easier to find a solution for it.
     * 
     * @param goals the set of goals from which a goal must be selected.
     * @return the goal selected.
     */
    private Fact selectGoal(BitSet goals) {
        int i = goals.nextSetBit(0);
        Fact fact = this.facts_table.get(i);
        i = goals.nextSetBit(i + 1);
        while (i >= 0) {
            final Fact fi = this.facts_table.get(i);
            if (fact.getLevel() < fi.getLevel()) {
                fact = fi;
            }
            i = goals.nextSetBit(i + 1);
        }
        return fact;
    }
    
    /**
     * Select a resolver from a set of resolvers. The resolver selection is
     * based on the level heuristics, i.e., choose always the resolver that
     * appears latest in the planning graph. it.
     * 
     * @param resolvers the set of resolvers from which a resolver must be selected.
     * @return the selected resolver according to the level based heuristics.
     */
    private Op selectResolver(final BitSet resolvers) {
        int i = resolvers.nextSetBit(0);
        Op resolver = this.ops_table.get(i);
        int lr = resolver.getLevel();
        i = resolvers.nextSetBit(i + 1);
        while (i >= 0) {
            final Op opi = this.ops_table.get(i);
            final int li = opi.getLevel();
            if (lr > li) {
                lr = li;
                resolver = opi;
            }
            i = resolvers.nextSetBit(i + 1);
        }
        
        if (resolver.getId() > this.ops_index) {
            this.a_tried++;
        } else {
            this.noop_tried++;
        }
        return resolver;
    }
    
    //-----------------------------------------------------------------------//
    // Methods used for planning graph expansion                             //
    //-----------------------------------------------------------------------// 
       
    /**
     * Returns the set of resolvers of a specific fact at a specified level of
     * the graph.
     * 
     * @param i the fact.
     * @param k the level of the graph.
     * @return the set of resolvers of the fact i at level k of the planning
     *         graph.
     */
    private BitSet getResolvers(final int i, final int k) {
        return this.resolvers.get(k - 1).getRow(i);
    }
   
    /**
     * Returns <code>true</code> if a operator is applicable to specified
     * level of the planning graph. An operator is applicable to proposition
     * level k of a graph if its preconditions are included and are mutex free
     * in the proposition level k.
     * 
     * @param op The operator to be tested.
     * @param k the level of graph where the test is done.
     * @return <code>true</code> if a operator is applicable to specified
     *         level of the planning graph; <code>false</code> otherwise.
     */
    private boolean isApplicable(final Op op, final int k) {
        final BitSet pre = op.getPositivePreconditon();
        return this.contains(pre, k) && this.isMutexFree(pre, k);

    }
    
    /**
     * Returns <code>true</code> if a set of facts is mutex free in a
     * specified proposition level of the planning graph.
     * 
     * @param facts the facts to be tested.
     * @param k the level of the graph at which the test is done.
     * @return <code>true</code> if a set of facts is mutex free in a
     *         specified proposition level of the planning graph,
     *         <code>false</code> otherwise.
     */
    private boolean isMutexFree(final BitSet facts, final int k) {
        boolean free = true;
        final BitMatrix mk = this.facts_mutex.get(k);
        int i = facts.nextSetBit(0);
        while (i >= 0 && free) {
            free = !mk.getRow(i).intersects(facts);
            i = facts.nextSetBit(i + 1);
        }
        return free;
    
    }
    
    /**
     * Returns <code>true</code> if a specified set of fact is contains in a
     * specific level of the graph.
     * 
     * @param facts the facts to be tested.
     * @param k the level of the graph where the test is done.
     * @return <code>true</code> if a specified set of fact is contains in a
     *         specific level of the graph; <code>false</code>otherwise.
     */
    private boolean contains(final BitSet facts, final int k) {
        final BitSet pi = (BitSet) this.facts_layers.get(k).clone();
        pi.and(facts);
        return pi.equals(facts);
    }
    
    /**
     * Expands the kth level planning graph.
     * 
     * @param k the level to expand.
     */
    private void expand(final int k) {
        if (!this.isLevelOff()) {
            final long t1 = System.nanoTime();
            // Clone the proposition level k to add noop actions to the new
            // actions layer
            final BitSet ak = (BitSet) this.facts_layers.get(k).clone();
            // Apply an logical or with the action level k - 1 to copy the
            // previous actions
            if (k > 0) {
                ak.or(this.ops_layers.get(k - 1));
            }
            this.ops_layers.add(ak);
            this.ops_mutex.add(new BitMatrix(this.ops_table.size()));
            final BitSet pk_1 = (BitSet) this.facts_layers.get(k).clone();
            this.facts_layers.add(pk_1);
            this.facts_mutex.add(new BitMatrix(this.facts_table_size));
            final BitMatrix rk = new BitMatrix(this.facts_table_size, this.ops_table_size);
            this.resolvers.add(rk);

            for (int i = ak.nextClearBit(this.ops_index); i >= 0
                        && i < this.ops_table_size; i = ak.nextClearBit(i + 1)) {
                final Op opi = this.ops_table.get(i);
                if (this.isApplicable(opi, k)) {
                    opi.setLevel(k);
                    ak.set(opi.getId(), true);
                    pk_1.or(opi.getPositiveEffect());
                    pk_1.or(opi.getNegativeEffect());
                }
            }

            // Udpate the level where the fact appear for the first time
            final BitSet newFacts = (BitSet) this.facts_layers.get(k).clone();
            newFacts.xor(pk_1);
            for (int i = newFacts.nextSetBit(0); i >= 0; i = newFacts.nextSetBit(i + 1)) {
                this.facts_table.get(i).setLevel(k);
            }

            // Update the resolver to speed up the search
            for (int i = pk_1.nextSetBit(0); i >= 0; i = pk_1.nextSetBit(i + 1)) {
                for (int j = ak.nextSetBit(0); j >= 0; j = ak.nextSetBit(j + 1)) {
                    if (this.ops_table.get(j).getPositiveEffect().get(i)) {
                        rk.set(i, j);
                    }
                }
            }

            // Finally we update the mutex of the level k
            long t2 = System.nanoTime();
            final BitMatrix mak = this.ops_mutex.get(k);
            for (int i = ak.nextSetBit(0); i >= 0; i = ak.nextSetBit(i + 1)) {
                for (int j = ak.nextSetBit(0); j >= 0; j = ak.nextSetBit(j + 1)) {
                    if (i > j && this.areMutexOps(i, j, k)) {
                        mak.set(i, j);
                        mak.set(j, i);
                    }
                }
            }

            final BitMatrix mpk_1 = this.facts_mutex.get(k + 1);
            for (int i = pk_1.nextSetBit(0); i >= 0; i = pk_1.nextSetBit(i + 1)) {
                for (int j = pk_1.nextSetBit(0); j >= 0; j = pk_1.nextSetBit(j + 1)) {
                    if (i > j && areMutexFact(i, j, k + 1)) {
                        mpk_1.set(i, j);
                        mpk_1.set(j, i);
                    }
                }
            }
            
            final long t3 = System.nanoTime();
            this.m_time +=  t3 - t2;
            final long lTime = t3 - t1;
            this.g_time += lTime;
            final int facts = this.facts_layers.get(k).cardinality();
            final int fm = this.facts_mutex.get(k).cardinality() / 2;
            final int ops = this.ops_layers.get(k).cardinality();
            final int om = this.ops_mutex.get(k).cardinality() / 2;
            System.out.printf("time: %4d, %8d facts, %10d and exlusive pairs (%2.4fs)\n",
                                    k, facts, fm, lTime / 1000000000.0);
            System.out.printf("            %8d ops, %12d exlusive pairs\n", ops, om);

            if (this.isLevelOff()) {
                System.out.print("\ngraph has leveled off! wave front mechanism is taking over\n\n");
            }

        } else {
            this.ops_layers.add(this.ops_layers.get(k - 1));
            this.ops_mutex.add(this.ops_mutex.get(k - 1));
            this.facts_layers.add(this.facts_layers.get(k));
            this.facts_mutex.add(this.facts_mutex.get(k));
            this.resolvers.add(this.resolvers.get(k - 1));
            System.out.printf("expanding wave front to level %4d\n", k);
        }
    }
    
    
   
    /**
     * Returns <code>true</code> if two facts are mutex at a specified level.
     * Two facts are mutex if at least one operator that produce the facts are
     * mutex in the previoud level k - 1.
     * 
     * @param i the first fact.
     * @param j the second fact.
     * @param k the level where the test must be done.
     * @return <code>true</code> if two facts are mutex at a specified level;
     *         <code>false</code> otherwise.
     */
    private boolean areMutexFact(int i, int j, int k) {  
        boolean mutex = false;
        if (k > 0) {
            mutex = true;
            BitMatrix mak = this.ops_mutex.get(k - 1);
            BitSet rak = this.getResolvers(i, k);
            BitSet rbk = this.getResolvers(j, k);
            int ra = rak.nextSetBit(0);
            while (ra >= 0 && mutex) {                   
                int rb = rbk.nextSetBit(0);
                while (rb >= 0 && mutex) {
                    mutex = mak.get(ra, rb);
                    rb = rbk.nextSetBit(rb + 1);
                }
                ra = rak.nextSetBit(ra + 1);
            }
        }        
        return mutex;
    }
    
    /**
     * Returns <code>true</code> if two operator are mutex at a specified
     * level. Two operators are mutex if the operators are dependent or an
     * operator has a mutex precondition at level k;
     * 
     * @param i the first fact.
     * @param j the second fact.
     * @param k the level where the test must be done.
     * @return <code>true</code> if two operator are mutex at a specified
     *         level; <code>false</code> otherwise.
     */
    private boolean areMutexOps(final int i, final int j, final int k) {
        if (!this.dependences.get(i, j)) {
            boolean mutex = false; 
            final BitSet ppi = this.ops_table.get(i).getPositivePreconditon();
            final BitSet ppj = this.ops_table.get(j).getPositivePreconditon();
            final BitMatrix mk = this.facts_mutex.get(k);
            int mi = ppi.nextSetBit(0);
            while (mi >= 0 && !mutex) {
                mutex = mk.getRow(mi).intersects(ppj);
                mi = ppi.nextSetBit(mi + 1);
            }
            return mutex;
        }
        return true;
    }
    
    /**
     * Returns <code>true</code> if the fixed point of the planning graph was
     * reached.
     * 
     * @return <code>true</code> if the fixed point of the planning graph was
     *         reached; <code>false</code> otherwise.
     */
    public boolean isLevelOff() {
        if (this.levelOff == -1) {
            if (this.facts_layers.size() > 3) {
                int last = this.facts_layers.size() - 1;
                final BitSet pn = this.facts_layers.get(last);
                final BitMatrix mpn = this.facts_mutex.get(last);
                final BitSet an = this.ops_layers.get(last - 2);
                final BitMatrix man = this.ops_mutex.get(last - 2);
                final BitSet pn_1 = this.facts_layers.get(last - 2);
                final BitMatrix mpn_1 = this.facts_mutex.get(last - 2);
                final BitSet an_1 = this.ops_layers.get(last - 3);
                final BitMatrix man_1 = this.ops_mutex.get(last - 3);
                if (pn.equals(pn_1) && an.equals(an_1) && mpn.equals(mpn_1)
                            && man.equals(man_1)) {
                    this.levelOff = last;
                    return true;
                }
            }
            return false;
        }
        return true;
    } 
   
    //-----------------------------------------------------------------------//
    // Methods used for planning preprocessing                               //
    //-----------------------------------------------------------------------//   
    
    /**
     * Executes the preprocessing of the problem, i.e, instantiates the
     * operators of the problem, conmputes the static dependence between
     * operators, init the firts level of the planning graph and the goal.
     * 
     * @throws InvalidExpException if an pddl expression of the goal or in an
     *             action definition is not accepted by the planner
     */
    private void preprocessing() throws InvalidExpException {
        long begin = System.nanoTime();
        System.out.println("Preprocessing ...\n");
        this.initFacts();
        this.initOps();
        this.initOpsDependences();
        this.initGoals();
        this.initPlanningGraph();
        this.initNogoodsTable();
        long end = System.nanoTime();
        this.p_time = (end - begin);
    }
    
    /**
     * Init the no goods goals table used to store goals search failure.
     */
    private void initNogoodsTable() {
        this.nogoods = new HashMap<Integer, Set<BitSet>>();
    }  
   
    /**
     * Enumerates all the facts of the planning problem.
     */
    private void initFacts() {
        this.facts_table =  new ArrayList<Fact>(100);
        Iterator<AtomicFormula> i = this.problem.predicatesIterator();
        while (i.hasNext()) {
            AtomicFormula skeleton = i.next();
            final List<Term> args = new ArrayList<Term>();
            for (Term arg: skeleton) {
                args.add(arg);
            }
            this.instantiateFacts(skeleton, args, new Substitution());
        }
        this.facts_table_size = this.facts_table.size();
    }
    
    /**
     * Instantiates all the fact from a specific atomic formula skeleton of the
     * problem.
     * 
     * @param skeleton the atomic formula skeleton to instantiate.
     * @param args the list of arguments of the atomic formula skeleton not yet
     *            instantiated.
     * @param sigma the map containing for each argument already instantiated its
     *            value.
     */
    private void instantiateFacts(final AtomicFormula skeleton,
                final List<Term> args,
                final Substitution sigma) {
        if (args.isEmpty()) {
            final Fact fact = new Fact(skeleton.apply(sigma), this.facts_table.size());
            this.facts_table.add(fact);
        } else {
            final Variable var = (Variable) args.get(0);
            final Set<Constant> values = this.problem.getTypedDomain(var.getTypeSet());
            for (Constant c : values) {
                sigma.bind(var, c);
                final List<Term> newArgs = new ArrayList<Term>(args);
                Term t = newArgs.remove(0);
                this.instantiateFacts(skeleton, newArgs, sigma);
            }
            
        }
    }

    
    /**
     * Enumerates all the operators of the planning problem.
     * 
     * @throws InvalidExpException if an expression of an operator of the
     *             problem is not an accepted pddl expression.
     */
    private void initOps() throws InvalidExpException {
        // Generate the noop action first so a noop action as the same index as
        // its corresponding fact.
        for (int i = 0; i < this.facts_table_size; i++) {
            this.addNewNoOp(this.facts_table.get(i));
        }
        this.ops_index = this.ops_table.size();
        Iterator<ActionDef> i = this.problem.actionsIterator();
        while (i.hasNext()) {
            ActionDef a = i.next();
            if (a.getActionID().equals(ActionID.ACTION)) {
                Action op = (Action) a;
                List<Term> parameters = new ArrayList<Term>(op.getParameters());
                this.instantiateOps(op, parameters, new Substitution());
            } 
        }
        this.ops_table_size = this.ops_table.size();
    }
    
    /**
     * Instantiates all the operator from a specific action of the problem.
     * 
     * @param action the atomic formula skeleton to instantiate.
     * @param param the list of parameters of the action not yet instantiated.
     * @param sigma the map containing for each parameter already instantiated its
     *            value.
     * @throws InvalidExpException if an invalid pddl expression is found in
     *             the action.
     */
    private void instantiateOps(final Action action,
                final List<Term> param,
                final Substitution sigma)
                throws InvalidExpException {

        if (param.isEmpty()) {
            this.addNewOp(action, sigma);
        } else {
            final Variable var = (Variable) param.get(0);
            final Set<Constant> values = this.problem.getTypedDomain(var.getTypeSet());
            for (Constant c : values) {
                sigma.bind(var, c);
                final List<Term> newParam = new ArrayList<Term>(param);
                newParam.remove(0);
                this.instantiateOps(action, newParam, sigma);
            }
        }
    }

    
    /**
     * Add a new op created from a specific pddl action.
     * 
     * @param action the action.
     * @param sigma the mapping between the parameters and their values.
     * @return <code>true</code> if a new op is added; <code>false</code> otherwise.
     * @throws InvalidExpException if the unexpected pddl expression occurs in
     *             the action definition.
     */
    private boolean addNewOp(Action action, Substitution sigma)
                throws InvalidExpException {
        boolean added = false;
        AtomicFormula name = new AtomicFormula(action.getName());
        for (Term p : action.getParameters()) {
            name.add((Constant) sigma.getBinding((Variable) p));
        }
        BitSet[] pre = expToBitSet(action.getPrecondition().apply(sigma)); 
        if (pre != null) {
            BitSet[] eff = expToBitSet(action.getEffect().apply(sigma));
            Op op = new Op(name, pre[0], pre[1], eff[0], eff[1], this.ops_table.size());
            this.ops_table.add(op);
            added = true;
        } 
        return added;

    }
    
    /**
     * Creates a new noop operator from a specific fact.
     * 
     * @param fact the fact.
     */
    private void addNewNoOp(final Fact fact) {
        AtomicFormula name = fact.clone();
        name.setPredicate("noop-" + name.getPredicate());
        BitSet pp = new BitSet(this.facts_table_size);
        pp.set(fact.getId());
        BitSet np = new BitSet(this.facts_table_size);
        BitSet pe = new BitSet(this.facts_table_size);
        pe.set(fact.getId());
        BitSet ne = new BitSet(this.facts_table_size);
        Op op = new Op(name, pp, np, pe, ne, this.ops_table.size());
        this.ops_table.add(op);
    }
    
    /**
     * Computes the static dependence between the operators of the planning problem.
     */
    private void initOpsDependences() {
        this.dependences =  new BitMatrix(this.ops_table.size());
        final int ops_size = this.ops_table.size();
        for (int i = this.ops_index; i < ops_size; i++) {   
            final Op opi = this.ops_table.get(i);
            for (int j = 0; j < this.ops_index; j++) {
                final Op opj = this.ops_table.get(j);
                if (opi.getId() > opj.getId() && are_dependent(opi, opj)) {
                    this.dependences.set(opi.getId(), opj.getId());
                    this.dependences.set(opj.getId(), opi.getId());
                }
            }   
        }
        for (int i = 0; i < ops_size; i++) {   
            final Op opi = this.ops_table.get(i);
            for (int j = this.ops_index; j < ops_size; j++) {
                final Op opj = this.ops_table.get(j);
                if (opi.getId() > opj.getId() && are_dependent(opi, opj)) {
                    this.dependences.set(opi.getId(), opj.getId());
                    this.dependences.set(opj.getId(), opi.getId());
                }
            }   
        }
        
    }
    
    /**
     * Returns <code>true</code> if two operators are dependent, i.e, if an
     * operator delete an precondition or an add effect of the other.
     * 
     * @param op1 The first operator.
     * @param op2 The second operator
     * @return <code>true</code> if two operators are dependent,
     *         <code>false</code> otherwise.
     */
    private boolean are_dependent(final Op op1, final Op op2) {
        return op1.getNegativeEffect().intersects(op2.getPositivePreconditon())
                    || op1.getNegativeEffect().intersects(op2.getPositiveEffect())
                    || op2.getNegativeEffect().intersects(op1.getPositivePreconditon())
                    || op2.getNegativeEffect().intersects(op1.getPositiveEffect());
    }
    
    /**
     * Init the planning graph, i.e., create the first fact level of the
     * planning graph.
     * 
     * @throws InvalidExpException if an invalid pddl expression is found in
     *             the pddl initial state  description of the problem.
     */
    private void initPlanningGraph() throws InvalidExpException {
        final BitSet p0 = new BitSet(this.facts_table_size);
        final List<InitEl> init = this.problem.getInit();
        for (InitEl el : init) {
            if (el.getExpID().equals(ExpID.ATOMIC_FORMULA)) {
                AtomicFormula predicate = (AtomicFormula) el;
                p0.set(this.facts_table.indexOf(predicate), true);
            } else {
                throw new InvalidExpException(el.getExpID());
            }
        }
        this.facts_layers.add(p0);
        this.facts_mutex.add(new BitMatrix(this.facts_table_size));
    }
    
    
    /**
     * Init the goals of the problem, i.e., convertes the goal of the in a pddl
     * format to inner format.
     * 
     * @throws InvalidExpException if an invalid pddl expression is found in
     *             the pddl goal description of the problem.
     */
    private void initGoals() throws InvalidExpException {
        final Exp goal = this.problem.getGoal();
        this.goals = new BitSet(this.facts_table_size);
        if (goal.getExpID().equals(ExpID.AND)) {
            final AndExp andExp = (AndExp) goal;
            for (Exp e : andExp) {
                if (e.getExpID().equals(ExpID.ATOMIC_FORMULA)) {
                    final AtomicFormula predicate = (AtomicFormula) e;
                    this.goals.set(this.facts_table.indexOf(predicate), true);
                } else {
                    throw new InvalidExpException(e.getExpID());
                }
            }
        } else {
            throw new InvalidExpException(goal.getExpID());
        }
    }
 
    //-----------------------------------------------------------------------//
    // Methods used for pddl expressions manipulation                        //
    //-----------------------------------------------------------------------// 
    
    /**
     * Transforms a pddl expression into a bit set representation.
     * 
     * @param exp the expression to transform.
     * @return a array of bit set with a length of 2 such the first bit set
     *         represents the positive fact of the expression and second bit set
     *         that represents the negative fact of the expression.
     * @throws InvalidExpException if an sub expression of the specified
     *             expression is not valid.
     */
    private BitSet[] expToBitSet(final Exp exp) 
            throws InvalidExpException {
        BitSet[] bitSet = new BitSet[2]; 
        bitSet[0] = new BitSet(this.facts_table_size);
        bitSet[1] = new BitSet(this.facts_table_size);
        HashSet<Literal> literals = null;
        
        literals = this.toLiteralSet(exp);
        if (literals != null) {
            for (Literal literal : literals) {
                if (literal.getExpID().equals(ExpID.ATOMIC_FORMULA)) {
                    AtomicFormula predicate = (AtomicFormula) literal;
                    final int index = this.facts_table.indexOf(predicate);
                    bitSet[0].set(index);
                } else {
                    NotAtomicFormula notExp = (NotAtomicFormula) literal;
                    AtomicFormula predicate = (AtomicFormula) notExp.getExp();
                    final int index = this.facts_table.indexOf(predicate);
                    bitSet[1].set(index);
                }
            }
        } else {
            bitSet = null;
        }
        return bitSet;
        
    }
    
    /**
     * Converts a ground pddl expression into a set of literals.
     * 
     * @param exp the expression that must be converted.
     * @return a set of literal that represents the specified expression.
     * @throws InvalidExpException if an sub expression of the specified
     *             expression is not valid.
     */
    private HashSet<Literal> toLiteralSet(final Exp exp) throws InvalidExpException {
        HashSet<Literal> literals = new HashSet<Literal>();

        Stack<Exp> stack = new Stack<Exp>();
        stack.add(exp);
        boolean failure = false;
        
        while (!stack.isEmpty() && !failure) {
            Exp e = stack.pop();
            switch (e.getExpID()) {
            case AND:
                AndExp andExp = (AndExp) e;
                for (Exp sexp : andExp) {
                    stack.push(sexp);
                }
                break;
            case ATOMIC_FORMULA:
                AtomicFormula p = (AtomicFormula) e;
                literals.add(p.clone());
                break;
            case F_COMP:
                FCompExp fcomp = (FCompExp) e;
                failure = !fcomp.evaluate();
                break;
            case NOT:
                NotExp notExp = (NotExp) e;
                Exp nexp = notExp.getExp();
                switch (nexp.getExpID()) {
                case F_COMP:
                    fcomp = (FCompExp) nexp;
                    failure = fcomp.evaluate();
                    break;
                default:
                    HashSet<Literal> pl = toLiteralSet(notExp.getExp());
                    if (pl != null) {
                        for (Literal l : pl) {
                            NotAtomicFormula f = new NotAtomicFormula((AtomicFormula) l);
                            literals.add(f);
                        }
                     } else {
                       failure = true;
                     }
                }
                break;
            default:
                throw new InvalidExpException(exp.getExpID());
            }
        }
        return failure ? null : literals;
    }
     
   
    
    //-----------------------------------------------------------------------//
    // Methods used to print                                                 //
    //-----------------------------------------------------------------------// 
    
    /**
     * Print a specified plan.
     * 
     * @param plan the plan to print.
     */
    private static void printPlan(Plan plan) {
        int i = 0;
        for (Set<AtomicFormula> layer : plan) {
            StringBuffer str = new StringBuffer();
            System.out.printf("time step %4d: ", i);
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
            System.out.println(str.toString());
            i++;
        }
        
    }
    
    /**
     * Print the ops table.
     */
    private void printOpsTable() {
        for (Op op : this.ops_table) {
            System.out.println(this.opToString(op));
        }
    }
    
    
    /**
     * Prints a propositions layer at a specified layer.
     * 
     * @param k the layer.
     */
    private void printPropAt(int k) {
        BitSet ops = this.facts_layers.get(k);
        for (int i = ops.nextSetBit(0); i >= 0; i = ops.nextSetBit(i + 1)) {
            System.out.println(this.facts_table.get(i));
        }
        System.out.println("");
    }
    
    /**
     * Prints a operators layer at a specified layer.
     * 
     * @param k the k layer.
     */
    private void printOpAt(int k) {
        BitSet ops = this.ops_layers.get(k);
        for (int i = ops.nextSetBit(0); i >= 0; i = ops.nextSetBit(i + 1)) {
            System.out.println(this.ops_table.get(i).getName());
        }
        System.out.println("");
    }
    
    /**
     * Prints a mutex operator layer at a specified layer.
     * 
     * @param k the layer.
     */
    private void printOpMutexAt(int k) {
        BitMatrix mutex = this.ops_mutex.get(k);
        BitSet ops = this.ops_layers.get(k);
        for (int i = ops.nextSetBit(0); i >= 0; i = ops.nextSetBit(i + 1)) {
            System.out.print(this.ops_table.get(i).getName() + "\n  mutex:");
            BitSet vector = mutex.getRow(i);
            for (int j = vector.nextSetBit(0); j >= 0; j = vector.nextSetBit(j+1)) {
                System.out.print(" " + this.ops_table.get(j).getName());
            }
            System.out.println("");
        }
    }
    
    /**
     * Print a mutex propositions layer at a specified layer.
     * 
     * @param k the layer.
     */
    private void printPropMutexAt(int k) {
        BitMatrix mutex = this.facts_mutex.get(k);
        BitSet props = this.facts_layers.get(k);
        for (int i = props.nextSetBit(0); i >= 0; i = props.nextSetBit(i + 1)) {
            System.out.print(this.facts_table.get(i) + "\n  mutex:");
            BitSet vector = mutex.getRow(i);
            for (int j = vector.nextSetBit(0); j >= 0; j = vector.nextSetBit(j+1)) {
                System.out.print(" " + this.facts_table.get(j));
            }
            System.out.println("");
        }
    }
    
    /**
     * Returns a string representation of a specified operator.
     * 
     * @param op the operator to convert.
     * @return the string representation of the specified operator.
     * @see java.lang.Object#toString()
     */
    public String opToString(Op op) {
        StringBuffer str = new StringBuffer();
        str.append("Op ");
        str.append(op.getName());
        str.append("\n");
        str.append("precondition:\n");
        str.append("(");
        if (!op.getPositivePreconditon().isEmpty()) {
            int i =op.getPositivePreconditon().nextSetBit(0);
            if (i >= 0) {
                str.append(this.facts_table.get(i));
            }
            i = op.getPositivePreconditon().nextSetBit(i + 1);
            while (i >= 0) {
                str.append(" AND ");
                str.append(this.facts_table.get(i));
                i = op.getPositivePreconditon().nextSetBit(i + 1);
            }
        }
        if (!op.getNegativePrecondition().isEmpty()) {
            int i = op.getNegativePrecondition().nextSetBit(0);
            if (i >= 0) {
                str.append(this.facts_table.get(i));
            }
            i = op.getNegativePrecondition().nextSetBit(i + 1);
            while (i >= 0) {
                str.append(" AND (NOT ");
                str.append(this.facts_table.get(i));
                str.append(")");
                i = op.getNegativePrecondition().nextSetBit(i + 1);
            }
        }
        str.append(")\n");
        str.append("effect:\n");
        str.append("(");
        if (!op.getPositiveEffect().isEmpty()) {
            int i = op.getPositiveEffect().nextSetBit(0);
            if (i >= 0) {
                str.append(this.facts_table.get(i));
            }
            i = op.getPositiveEffect().nextSetBit(i + 1);
            while (i >= 0) {
                str.append(" AND ");
                str.append(this.facts_table.get(i));
                i = op.getPositiveEffect().nextSetBit(i + 1);
            }
        }
        if (!op.getNegativeEffect().isEmpty()) {
            int i = op.getNegativeEffect().nextSetBit(0);
            if (i >= 0) {
                str.append(this.facts_table.get(i));
            }
            i = op.getNegativeEffect().nextSetBit(i + 1);
            while (i >= 0) {
                str.append(" AND (NOT ");
                str.append(this.facts_table.get(i));
                str.append(")");
                i = op.getNegativeEffect().nextSetBit(i + 1);
            }
        }
        str.append(")");
        return str.toString();
        
    }
}
