package com.epam.deltix.util.jcg;

/**
 * Supports all possible variations of try-catch-finally statement.
 */
public interface JTryStatement extends JStatement {
    /**
     * Returns a compound statement of try{} clause.
     */
    public JCompoundStatement tryStmt();

    /**
     * Adds a catch clause for the specified Throwable.
     * <p>
     * Adding the same Throwable twice causes an exception.
     * </p>
     *
     * @param t       Throwable class
     * @param varName name of Throwable variable to declare
     */
    public JCompoundStatement addCatch(Class<? extends Throwable> t, String varName);

    /**
     * Returns a throwable variable of the specificed catch clause
     * <p>
     * Throws an exception if addCatch was not called for the specified <code>t</code>.
     * </p>
     *
     * @param t Throwable class
     */
    public JLocalVariable catchVariable(Class<? extends Throwable> t);

    /**
     * Adds a finally clause.
     * <p>
     * Calling of the method twice causes an exception.
     * </p>
     */
    public JCompoundStatement addFinally();
}
