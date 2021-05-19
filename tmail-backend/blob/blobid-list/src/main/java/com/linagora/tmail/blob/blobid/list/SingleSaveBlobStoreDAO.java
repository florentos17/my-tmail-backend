package com.linagora.tmail.blob.blobid.list;

import java.io.InputStream;

import javax.inject.Inject;

import org.apache.james.blob.api.BlobId;
import org.apache.james.blob.api.BlobStoreDAO;
import org.apache.james.blob.api.BucketName;
import org.apache.james.blob.api.ObjectNotFoundException;
import org.apache.james.blob.api.ObjectStoreException;
import org.apache.james.blob.api.ObjectStoreIOException;
import org.reactivestreams.Publisher;

import com.google.common.io.ByteSource;

import reactor.core.publisher.Mono;

public class SingleSaveBlobStoreDAO implements BlobStoreDAO {
    private final BlobStoreDAO blobStoreDAO;
    private final BlobIdList blobIdList;
    private final BucketName defaultBucketName;

    @Inject
    public SingleSaveBlobStoreDAO(BlobStoreDAO blobStoreDAO,
                                  BlobIdList blobIdList,
                                  BucketName defaultBucketName) {
        this.blobStoreDAO = blobStoreDAO;
        this.blobIdList = blobIdList;
        this.defaultBucketName = defaultBucketName;
    }

    @Override
    public InputStream read(BucketName bucketName, BlobId blobId) throws ObjectStoreIOException, ObjectNotFoundException {
        return blobStoreDAO.read(bucketName, blobId);
    }

    @Override
    public Publisher<byte[]> readBytes(BucketName bucketName, BlobId blobId) {
        return blobStoreDAO.readBytes(bucketName, blobId);
    }

    @Override
    public Mono<Void> save(BucketName bucketName, BlobId blobId, byte[] data) {
        return Mono.from(blobIdList.isStored(blobId))
                .flatMap(isStored -> {
                    if (isStored.equals(true)) {
                        return Mono.empty();
                    }
                    return Mono.from(blobStoreDAO.save(bucketName, blobId, data))
                            .then(Mono.from(blobIdList.store(blobId)))
                            .then();
                });
    }

    @Override
    public Publisher<Void> save(BucketName bucketName, BlobId blobId, InputStream inputStream) {
        return Mono.from(blobIdList.isStored(blobId))
                .flatMap(isStored -> {
                    if (isStored.equals(true)) {
                        return Mono.empty();
                    }
                    return Mono.from(blobStoreDAO.save(bucketName, blobId, inputStream))
                            .then(Mono.from(blobIdList.store(blobId)))
                            .then();
                });
    }

    @Override
    public Publisher<Void> save(BucketName bucketName, BlobId blobId, ByteSource content) {
        return Mono.from(blobIdList.isStored(blobId))
                .flatMap(isStored -> {
                    if (isStored.equals(true)) {
                        return Mono.empty();
                    }
                    return Mono.from(blobStoreDAO.save(bucketName, blobId, content))
                            .then(Mono.from(blobIdList.store(blobId)))
                            .then();
                });
    }

    @Override
    public Mono<Void> delete(BucketName bucketName, BlobId blobId) {
        if (defaultBucketName.equals(bucketName)) {
            return Mono.error(new ObjectStoreException("Can not delete in the default bucket when single save is enabled"));
        } else {
            return Mono.from(blobStoreDAO.delete(bucketName, blobId));
        }
    }

    @Override
    public Publisher<Void> deleteBucket(BucketName bucketName) {
        return blobStoreDAO.deleteBucket(bucketName);
    }
}
