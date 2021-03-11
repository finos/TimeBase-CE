package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

/**
 *
 */
public final class RelationExpression extends ComplexExpression {
    public final OrderRelation             relation;

    public RelationExpression (long location, OrderRelation relation, Expression left, Expression right) {
        super (location, left, right);
        this.relation = relation;
    }

    public RelationExpression (OrderRelation relation, Expression left, Expression right) {
        this (NO_LOCATION, relation, left, right);
    }

    @Override
    protected void              print (int outerPriority, StringBuilder s) {
        String  op;

        switch (relation) {
            case GE:    op = " >= ";    break;
            case GT:    op = " > ";     break;
            case LE:    op = " <= ";    break;
            case LT:    op = " < ";     break;
            default:    throw new RuntimeException ();
        }

        printBinary (outerPriority, op, OpPriority.RELATIONAL, InfixAssociation.LEFT, s);
    }
    
    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return (
            super.equals (obj) &&
            relation == ((RelationExpression) obj).relation
        );
    }

    @Override
    public int                      hashCode () {
        return (super.hashCode () * 41 + relation.hashCode ());
    }
}
