package dev.ragcrawler.crawler.output;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.ragcrawler.crawler.parsing.OutputChunk;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class JsonlChunkWriter implements Closeable {

    private final ObjectMapper mapper;
    private final JsonGenerator generator;
    private final OutputStream out;
    private final Lock lock = new ReentrantLock();

    public JsonlChunkWriter(Path path) throws IOException {
        this.out = Files.newOutputStream(path);
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        JsonFactory factory = mapper.getFactory();
        this.generator = factory.createGenerator(out);
    }

    public void writeChunk(OutputChunk chunk) throws IOException {
        lock.lock();
        try {
            mapper.writeValue(generator, chunk);
            generator.writeRaw('\n');
            generator.flush();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() throws IOException {
        generator.close();
        out.close();
    }
}

