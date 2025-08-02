package com.mastercard.ids.fts.repository;

import com.mastercard.ids.fts.model.InboundFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<InboundFile, String> {

    @Query("SELECT f.fileId FROM InboundFile f WHERE f.fileDownloadStatus ILIKE 'Completed' AND f.fileUploadStatus ILIKE 'Completed'")
    List<String> findExistingCompletedFileIds();

    @Query("SELECT f FROM InboundFile f WHERE ((f.fileDownloadStatus NOT LIKE 'Completed' AND f.fileDownloadStatus NOT LIKE 'Purged') OR f.fileUploadStatus NOT LIKE 'Completed') AND f.abortFile = false")
    List<InboundFile> findNotCompletedFiles();

    @Query("SELECT count(*) FROM InboundFile f WHERE (f.fileDownloadStatus <> 'Completed' OR f.fileUploadStatus <> 'Completed') AND f.abortFile = false")
    int findNotCompletedFileCount();

    @Transactional
    @Modifying
    @Query("UPDATE InboundFile f SET f.fileDownloadStatus = :downloadStatus, f.fileUploadStatus = :uploadStatus WHERE f.fileId = :fileId")
    void updateFileStatusesByFileId(@Param("fileId") String fileId,
                                    @Param("downloadStatus") String downloadStatus,
                                    @Param("uploadStatus") String uploadStatus);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("""
                UPDATE InboundFile f
                SET f.fileDownloadStatus = :downloadStatus,
                    f.fileUploadStatus = :uploadStatus,
                    f.retryCount = f.retryCount + 1,
                    f.abortFile = CASE WHEN f.retryCount + 1 >= 3 THEN true ELSE f.abortFile END
                WHERE f.fileId = :fileId
            """)
    void updateAsFailedAndIncrementRetry(@Param("fileId") String fileId,
                                         @Param("downloadStatus") String downloadStatus,
                                         @Param("uploadStatus") String uploadStatus);
}
