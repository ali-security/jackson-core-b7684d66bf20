package com.fasterxml.jackson.core.read;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class ParserErrorHandlingTest
    extends com.fasterxml.jackson.core.BaseTest
{
    public void testInvalidKeywordsBytes() throws Exception {
        _testInvalidKeywords(MODE_INPUT_STREAM);
        _testInvalidKeywords(MODE_INPUT_STREAM_THROTTLED);
        _testInvalidKeywords(MODE_DATA_INPUT);
    }
    
    public void testInvalidKeywordsChars() throws Exception {
        _testInvalidKeywords(MODE_READER);
    }

    // Tests for [core#105] ("eager number parsing misses errors")
    public void testMangledNumbersBytes() throws Exception {
        _testMangledNumbers(MODE_INPUT_STREAM);
        _testMangledNumbers(MODE_INPUT_STREAM_THROTTLED);
        _testInvalidKeywords(MODE_DATA_INPUT);
    }

    public void testMangledNumbersChars() throws Exception {
        _testMangledNumbers(MODE_READER);
    }
    
    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */
    
    private void _testInvalidKeywords(int mode) throws Exception
    {
        doTestInvalidKeyword1(mode, "nul");
        doTestInvalidKeyword1(mode, "Null");
        doTestInvalidKeyword1(mode, "nulla");
        doTestInvalidKeyword1(mode, "fal");
        doTestInvalidKeyword1(mode, "False");
        doTestInvalidKeyword1(mode, "fals0");
        doTestInvalidKeyword1(mode, "falsett0");
        doTestInvalidKeyword1(mode, "tr");
        doTestInvalidKeyword1(mode, "truE");
        doTestInvalidKeyword1(mode, "treu");
        doTestInvalidKeyword1(mode, "trueenough");
        doTestInvalidKeyword1(mode, "C");

        // Multi-byte (non-ASCII) identifier chars right after keyword
        doTestInvalidKeyword1(mode, "true\u00E9");
        doTestInvalidKeyword1(mode, "null\u00E9");
        doTestInvalidKeyword1(mode, "false\u4E2Dx");

        // Multi-byte (non-ASCII) identifier char at start of token
        doTestInvalidKeyword1(mode, "\u00E9abc");
        doTestInvalidKeyword1(mode, "\u4E2Dx");

        // Multi-byte char that is NOT part of token (NBSP): after keyword, or at value start
        _testNonTokenChar(mode, "[true\u00A0]");
        _testNonTokenChar(mode, "{\"a\":null\u00A0}");
        _testNonTokenChar(mode, "[\u00A0]");
        _testNonTokenChar(mode, "{\"a\":\u00A0}");
    }

    private void doTestInvalidKeyword1(int mode, String value)
        throws IOException
    {
        String doc = "{ \"key1\" : "+value+" }";
        JsonParser p = createParser(mode, doc);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        // Note that depending on parser impl, we may
        // get the exception early or late...
        try {
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            p.nextToken();
            fail("Expected an exception for malformed value keyword");
        } catch (JsonParseException jex) {
            verifyException(jex, "Unrecognized token");
            verifyException(jex, value);
        } finally {
            p.close();
        }

        // Try as root-level value as well:
        doc = value + " "; // may need space after for DataInput
        p = createParser(mode, doc);
        try {
            p.nextToken();
            fail("Expected an exception for malformed value keyword");
        } catch (JsonParseException jex) {
            verifyException(jex, "Unrecognized token");
            verifyException(jex, value);
        } finally {
            p.close();
        }
    }

    // Must be reported as an error: not skipped, nor mis-decoded
    private void _testNonTokenChar(int mode, String doc) throws IOException
    {
        JsonParser p = createParser(mode, doc);
        try {
            while (p.nextToken() != null) { }
            fail("Expected an exception for invalid non-token char; doc: "+doc);
        } catch (JsonParseException jex) {
            verifyException(jex, "Unexpected character");
        } finally {
            p.close();
        }
    }

    private void _testMangledNumbers(int mode) throws Exception
    {
        String doc = "123true";
        JsonParser p = createParser(mode, doc);
        try {
            JsonToken t = p.nextToken();
            fail("Should have gotten an exception; instead got token: "+t);
        } catch (JsonParseException e) {
            verifyException(e, "expected space");
        }
        p.close();

        // Also test with floats
        doc = "1.5false";
        p = createParser(mode, doc);
        try {
            JsonToken t = p.nextToken();
            fail("Should have gotten an exception; instead got token: "+t);
        } catch (JsonParseException e) {
            verifyException(e, "expected space");
        }
        p.close();
    }
}
