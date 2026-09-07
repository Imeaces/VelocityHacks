package org.imeaces.levellogin.util;

import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class AdditionalPathDirectoryStream implements DirectoryStream<Path> {

    private final DirectoryStream<Path> delegate;
    private final Path additional;

    public AdditionalPathDirectoryStream(
            DirectoryStream<Path> delegate,
            Path additional
    ) {
        this.delegate = delegate;
        this.additional = additional;
    }

    @Override
    public @NonNull Iterator<Path> iterator() {
        Iterator<Path> original = delegate.iterator();

        return new Iterator<>() {
            private boolean added;

            @Override
            public boolean hasNext() {
                return original.hasNext() || !added;
            }

            @Override
            public Path next() {
                if (!added) {
                    added = true;
                    return additional;
                }

                if (original.hasNext()) {
                    return original.next();
                }

                throw new NoSuchElementException();
            }
        };
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
