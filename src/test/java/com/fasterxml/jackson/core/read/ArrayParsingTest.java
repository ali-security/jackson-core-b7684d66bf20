package com.fasterxml.jackson.core.read;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.core.io.SerializedString;

/**
 * Tests for array parsing
 */
public class ArrayParsingTest extends BaseTest
{
    private final static String DEPTH_EXCEEDED_MSG =
            "Depth (1001) exceeds the maximum allowed nesting depth (1000)";

    private final static SerializableString FIELD_A = new SerializedString("a");

    // Ways of advancing the parser; each one goes through a different
    // child-context creation site in the parser implementations
    private final static int ADVANCE_NEXT_TOKEN = 0;
    private final static int ADVANCE_NEXT_FIELD_NAME = 1;
    private final static int ADVANCE_NEXT_TEXT_VALUE = 2;
    private final static int ADVANCE_NEXT_INT_VALUE = 3;
    private final static int ADVANCE_NEXT_LONG_VALUE = 4;
    private final static int ADVANCE_NEXT_BOOLEAN_VALUE = 5;
    private final static int ADVANCE_NEXT_FIELD_NAME_MATCH = 6;

    private final static int[] ALL_ADVANCE_METHODS = new int[] {
        ADVANCE_NEXT_TOKEN,
        ADVANCE_NEXT_FIELD_NAME,
        ADVANCE_NEXT_TEXT_VALUE,
        ADVANCE_NEXT_INT_VALUE,
        ADVANCE_NEXT_LONG_VALUE,
        ADVANCE_NEXT_BOOLEAN_VALUE,
        ADVANCE_NEXT_FIELD_NAME_MATCH
    };

    public void testDeepNesting() throws Exception
    {
        final String DOC = createDeepNestedDoc(1050);
        JsonParser jp = createParserUsingStream(new JsonFactory(), DOC, "UTF-8");
        try {
            JsonToken jt;
            while ((jt = jp.nextToken()) != null) {

            }
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            assertEquals("Depth (1001) exceeds the maximum allowed nesting depth (1000)", e.getMessage());
        }
        jp.close();
    }

    // Same as above but for all blocking parser implementations
    // (byte stream, throttled byte stream, Reader, DataInput)
    public void testDeepNestingAllModes() throws Exception
    {
        final String DOC = createDeepNestedDoc(1050);
        for (int i = 0; i < ALL_MODES.length; i++) {
            _testDepthExceeded(ALL_MODES[i], DOC, ADVANCE_NEXT_TOKEN);
        }
    }

    // Arrays only: exactly at the limit must still be accepted
    public void testDeepArrayNestingAtLimit() throws Exception
    {
        final String DOC = createDeepArrayDoc(1000);
        for (int i = 0; i < ALL_MODES.length; i++) {
            final int mode = ALL_MODES[i];
            JsonParser p = createParser(mode, DOC);
            int maxDepth = 0;
            for (int j = 0; j < 1000; j++) {
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                maxDepth = Math.max(maxDepth, p.getParsingContext().getNestingDepth());
            }
            for (int j = 0; j < 1000; j++) {
                assertToken(JsonToken.END_ARRAY, p.nextToken());
            }
            assertEquals(1000, maxDepth);
            assertEquals(0, p.getParsingContext().getNestingDepth());
            if (mode != MODE_DATA_INPUT) { // no look-ahead for DataInput
                assertNull(p.nextToken());
            }
            p.close();
        }
    }

    // Arrays only: one past the limit, and a much deeper (attack-sized) document
    public void testDeepArrayNestingOverLimit() throws Exception
    {
        for (int i = 0; i < ALL_MODES.length; i++) {
            _testDepthExceeded(ALL_MODES[i], createDeepArrayDoc(1001), ADVANCE_NEXT_TOKEN);
            _testDepthExceeded(ALL_MODES[i], createDeepArrayDoc(50000), ADVANCE_NEXT_TOKEN);
        }
    }

    // Objects only: object values following field names
    public void testDeepObjectNesting() throws Exception
    {
        for (int i = 0; i < ALL_MODES.length; i++) {
            final int mode = ALL_MODES[i];
            // 1000 levels fine
            JsonParser p = createParser(mode, createDeepObjectDoc(1000));
            int maxDepth = 0;
            for (int j = 0; j < 1000; j++) {
                assertToken(JsonToken.START_OBJECT, p.nextToken());
                maxDepth = Math.max(maxDepth, p.getParsingContext().getNestingDepth());
                if (j < 999) {
                    assertToken(JsonToken.FIELD_NAME, p.nextToken());
                }
            }
            assertEquals(1000, maxDepth);
            for (int j = 0; j < 1000; j++) {
                assertToken(JsonToken.END_OBJECT, p.nextToken());
            }
            p.close();

            // 1001 levels not
            _testDepthExceeded(mode, createDeepObjectDoc(1001), ADVANCE_NEXT_TOKEN);
        }
    }

    // Verify all "nextXxx()" accessors also enforce the limit, for arrays
    // and objects following field names, as well as values in arrays
    public void testDeepNestingWithNextXxxAccessors() throws Exception
    {
        final String MIXED_DOC = createDeepNestedDoc(1050);
        final String OBJECT_DOC = createDeepObjectDoc(1050);
        for (int i = 0; i < ALL_MODES.length; i++) {
            for (int j = 0; j < ALL_ADVANCE_METHODS.length; j++) {
                _testDepthExceeded(ALL_MODES[i], MIXED_DOC, ALL_ADVANCE_METHODS[j]);
                _testDepthExceeded(ALL_MODES[i], OBJECT_DOC, ALL_ADVANCE_METHODS[j]);
            }
        }
    }

    public void testNestingDepthTracking() throws Exception
    {
        for (int i = 0; i < ALL_MODES.length; i++) {
            JsonParser p = createParser(ALL_MODES[i], "[ { \"a\" : [ ] } ] ");
            assertEquals(0, p.getParsingContext().getNestingDepth());
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            assertEquals(1, p.getParsingContext().getNestingDepth());
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertEquals(2, p.getParsingContext().getNestingDepth());
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertEquals(2, p.getParsingContext().getNestingDepth());
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            assertEquals(3, p.getParsingContext().getNestingDepth());
            assertToken(JsonToken.END_ARRAY, p.nextToken());
            assertEquals(2, p.getParsingContext().getNestingDepth());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
            assertEquals(1, p.getParsingContext().getNestingDepth());
            assertToken(JsonToken.END_ARRAY, p.nextToken());
            assertEquals(0, p.getParsingContext().getNestingDepth());
            p.close();
        }
    }

    public void testDefaultConstraints() throws Exception
    {
        assertEquals(1000, StreamReadConstraints.DEFAULT_MAX_DEPTH);
        assertEquals(1000, StreamReadConstraints.defaults().getMaxNestingDepth());
        assertEquals(1000, StreamReadConstraints.builder().build().getMaxNestingDepth());
        assertEquals(5, StreamReadConstraints.builder().maxNestingDepth(5).build().getMaxNestingDepth());
        try {
            StreamReadConstraints.builder().maxNestingDepth(-1);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            verifyException(e, "Cannot set maxNestingDepth to a negative value");
        }
        StreamReadConstraints.defaults().validateNestingDepth(1000);
        try {
            StreamReadConstraints.defaults().validateNestingDepth(1001);
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            assertEquals(DEPTH_EXCEEDED_MSG, e.getMessage());
        }
    }

    private void _testDepthExceeded(int mode, String doc, int advanceMethod) throws Exception
    {
        JsonParser p = createParser(mode, doc);
        try {
            // documents are all deeper than the limit so we must fail
            // well before running out of content
            while (true) {
                _advance(p, advanceMethod);
                if (p.currentToken() == null) {
                    break;
                }
            }
            fail("expected StreamConstraintsException (mode "+mode+", method "+advanceMethod+")");
        } catch (StreamConstraintsException e) {
            assertEquals(DEPTH_EXCEEDED_MSG, e.getMessage());
        }
        p.close();
    }

    private void _advance(JsonParser p, int advanceMethod) throws Exception
    {
        switch (advanceMethod) {
        case ADVANCE_NEXT_TOKEN:
            p.nextToken();
            return;
        case ADVANCE_NEXT_FIELD_NAME:
            p.nextFieldName();
            return;
        case ADVANCE_NEXT_TEXT_VALUE:
            p.nextTextValue();
            return;
        case ADVANCE_NEXT_INT_VALUE:
            p.nextIntValue(0);
            return;
        case ADVANCE_NEXT_LONG_VALUE:
            p.nextLongValue(0L);
            return;
        case ADVANCE_NEXT_BOOLEAN_VALUE:
            p.nextBooleanValue();
            return;
        case ADVANCE_NEXT_FIELD_NAME_MATCH:
            p.nextFieldName(FIELD_A);
            return;
        }
        throw new IllegalArgumentException("Unknown advance method "+advanceMethod);
    }

    private String createDeepNestedDoc(final int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < depth; i++) {
            sb.append("{ \"a\": [");
        }
        sb.append(" \"val\" ");
        for (int i = 0; i < depth; i++) {
            sb.append("]}");
        }
        sb.append("]");
        return sb.toString();
    }

    private String createDeepArrayDoc(final int depth) {
        StringBuilder sb = new StringBuilder(depth * 2 + 1);
        for (int i = 0; i < depth; i++) {
            sb.append('[');
        }
        for (int i = 0; i < depth; i++) {
            sb.append(']');
        }
        sb.append(' ');
        return sb.toString();
    }

    private String createDeepObjectDoc(final int depth) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth - 1; i++) {
            sb.append("{\"a\":");
        }
        sb.append("{}");
        for (int i = 0; i < depth - 1; i++) {
            sb.append('}');
        }
        sb.append(' ');
        return sb.toString();
    }
}
