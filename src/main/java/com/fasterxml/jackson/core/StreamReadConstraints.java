package com.fasterxml.jackson.core;

import com.fasterxml.jackson.core.exc.StreamConstraintsException;

public class StreamReadConstraints
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    public static final int DEFAULT_MAX_DEPTH = 1000;

    private static final StreamReadConstraints DEFAULT =
        new StreamReadConstraints(DEFAULT_MAX_DEPTH);

    protected final int _maxNestingDepth;

    StreamReadConstraints(int maxNestingDepth) {
        _maxNestingDepth = maxNestingDepth;
    }

    public static StreamReadConstraints defaults() {
        return DEFAULT;
    }

    public static Builder builder() {
        return new Builder();
    }

    public int getMaxNestingDepth() {
        return _maxNestingDepth;
    }

    public void validateNestingDepth(int depth) throws StreamConstraintsException
    {
        if (depth > _maxNestingDepth) {
            throw new StreamConstraintsException(String.format("Depth (%d) exceeds the maximum allowed nesting depth (%d)",
                    depth, _maxNestingDepth));
        }
    }

    public static final class Builder {
        private int maxNestingDepth = DEFAULT_MAX_DEPTH;

        Builder() { }

        public Builder maxNestingDepth(int maxNestingDepth) {
            if (maxNestingDepth < 0) {
                throw new IllegalArgumentException("Cannot set maxNestingDepth to a negative value");
            }
            this.maxNestingDepth = maxNestingDepth;
            return this;
        }

        public StreamReadConstraints build() {
            return new StreamReadConstraints(maxNestingDepth);
        }
    }
}
