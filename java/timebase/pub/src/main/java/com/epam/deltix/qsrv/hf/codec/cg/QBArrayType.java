package com.epam.deltix.qsrv.hf.codec.cg;

import com.epam.deltix.qsrv.hf.codec.ArrayTypeUtil;
import com.epam.deltix.qsrv.hf.codec.CodecUtils;
import com.epam.deltix.qsrv.hf.codec.MessageSizeCodec;
import com.epam.deltix.qsrv.hf.pub.md.ClassDataType;
import com.epam.deltix.util.jcg.JCompoundStatement;
import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JLocalVariable;

import java.lang.reflect.Modifier;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

/**
 *
 */
public class QBArrayType extends QBoundType<QArrayType> {
    private static final JExpr ZERO_INTEGER = CTXT.intLiteral(0);
    private static final JExpr ONE_INTEGER = CTXT.intLiteral(1);

    private final CGContext context;
    private Class<?> elementType;


    public QBArrayType(QArrayType qType, final Class<?> javaType, Class<?> elementType, QAccessor accessor, CGContext context) {
        super(qType, javaType, accessor);

        this.elementType = elementType;
        this.context = context;
    }

    @Override
    public void decode(JExpr input, JCompoundStatement addTo) {
        // raw size
        final JCompoundStatement stmt = CTXT.compStmt();
        JExpr manager = context.lookupContainer.access(context.lookupContainer.lookupVar(CodecGenerator.MANAGER_NAME));

        final JLocalVariable size = stmt.addVar(Modifier.FINAL, int.class, "size",
                CTXT.staticCall(MessageSizeCodec.class, "read", input));

        // Do Not throw exception under Java
        final boolean isThrowException = !qType.isNullable();
        final JCompoundStatement elseStmt = CTXT.compStmt();
        stmt.add(
                CTXT.ifStmt(CTXT.binExpr(size, "==", ZERO_INTEGER),
                        (!isThrowException) ?
                                writeNullNoCheck() :
                                QCGHelpers.throwISX(String.format("cannot write null to not nullable field '%s'", accessor.getFieldName())),
                        elseStmt));

        // MDI limit
        final JLocalVariable maxPosition = elseStmt.addVar(Modifier.FINAL, int.class, "maxPosition",
                CTXT.binExpr(input.call("getPosition"), "+", size));
        // array length
        final JLocalVariable len = elseStmt.addVar(Modifier.FINAL, int.class, "len",
                CTXT.staticCall(MessageSizeCodec.class, "read", input));

        elseStmt.add(accessor.write( manager.call("use", CTXT.classLiteral(javaBaseType) ,len).cast(javaBaseType)));

        elseStmt.add(accessor.read().call("setSize", len));

        // decode all elements in a cycle
        final JLocalVariable i = elseStmt.addVar(0, int.class, "i");
        final QBoundType underline = createElementAccessor(i);
        final JCompoundStatement forBody = CTXT.compStmt();
        underline.decode(input, forBody);
        // assert NOT NULL in Java case
        if (!underline.qType.isNullable() && underline.hasNullLiteral()) {
            forBody.add(CTXT.assertStmt(underline.readIsNull(false), CTXT.stringLiteral(String.format("'%s[]' field array element is not nullable", accessor.getFieldName()))));
        }

        elseStmt.add(CTXT.forStmt(i.assignExpr(ZERO_INTEGER),
                CTXT.binExpr(CTXT.binExpr(i, "<", len), "&&", CTXT.binExpr(input.call("getPosition"), "<", maxPosition)),
                i.getAndInc(),
                forBody
        ));

        // ignore original object field value

        addTo.add(stmt);
    }
    @java.lang.SuppressWarnings("unchecked")
    @Override
    public void encode(JExpr output, JCompoundStatement addTo) {
        final JCompoundStatement stmt = CTXT.compStmt();

        if (qType.isNullable()) {
            final JCompoundStatement encodeNull = CTXT.compStmt();
            qType.encodeNull(output, encodeNull);
            addTo.add(
                    CTXT.ifStmt(
                            readIsNull(true),
                            encodeNull,
                            stmt
                    )
            );
        } else
            addTo.add(stmt);

        // backup position and skip one byte to later save the field size there
        final JLocalVariable pos = stmt.addVar(Modifier.FINAL, int.class, "pos0", output.call("getPosition"));
        stmt.add(output.call("skip", ONE_INTEGER));

        // array length
        final JLocalVariable len = stmt.addVar(Modifier.FINAL, int.class, "len", accessor.read().call("size"));
        stmt.add(CTXT.staticCall(MessageSizeCodec.class, "write", len, output));

        // iterate over elements
        final JLocalVariable i = stmt.addVar(0, int.class, "i");
        final QBoundType underline = createElementAccessor(i);
        final JCompoundStatement forBody = CTXT.compStmt();

        if (!underline.qType.isNullable() && underline.hasNullLiteral() && !(underline.qType.dt instanceof ClassDataType)) {
            forBody.add(
                    CTXT.ifStmt(underline.readIsNull(true),
                            QCGHelpers.throwIAX(String.format("'%s' field array element is not nullable", accessor.getFieldName()))
                    )
            );
        }

        if (underline.hasConstraint()) {
            forBody.add(
                    CTXT.ifStmt(
                            underline.readIsConstraintViolated(),
                            QCGHelpers.throwIAX(CTXT.sum(CTXT.stringLiteral(getFieldDescription() + " == "),
                                            CTXT.staticCall(String.class, "valueOf", underline.accessor.read())))
                    ));
        }

        underline.encode(output, forBody);
        stmt.add(CTXT.forStmt(i.assignExpr(ZERO_INTEGER),
                CTXT.binExpr(i, "<", len),
                i.getAndInc(),
                forBody
        ));


        // rewind back and store raw field size
        stmt.add(CTXT.staticCall(CodecUtils.class, "storeFieldSize", pos, output));
    }

    private QBoundType createElementAccessor(JExpr i) {
        final Class<?> cls = ArrayTypeUtil.getUnderline(javaBaseType);
        final QAArrayList arrayList = new QAArrayList(accessor.read(), i, ArrayTypeUtil.getUnderlineBoxed(javaBaseType), accessor.getFieldName(), accessor.getSchemaFieldName());
        return CodecGenerator.getPrimitiveValue((QPrimitiveType) QType.forDataType(qType.dt.getElementDataType()), cls, elementType, arrayList, context);
    }

    // TODO: it is a hack
    private String getFieldDescription() {
        final String s = accessor.getFieldDescription();
        return s.substring(s.lastIndexOf(' ') + 1);
    }
}
