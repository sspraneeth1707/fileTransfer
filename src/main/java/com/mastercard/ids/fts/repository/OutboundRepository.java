package com.mastercard.ids.fts.repository;

import com.mastercard.ids.fts.model.InboundFile;
import com.mastercard.ids.fts.model.OutboundFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface OutboundRepository extends JpaRepository<OutboundFile, String>{

    @Query("SELECT f FROM OutboundFile f WHERE (f.fileDownloadStatus NOT LIKE 'Completed' OR f.fileUploadStatus NOT LIKE 'Completed') AND f.abortFile = false")
    List<OutboundFile> findNotCompletedFiles();

    @Transactional
    @Modifying
    @Query("UPDATE OutboundFile f SET f.fileDownloadStatus = :downloadStatus, f.fileUploadStatus = :uploadStatus WHERE f.fileId = :fileId")
    void updateFileStatusesByFileId(@Param("fileId") String fileId,
                                    @Param("downloadStatus") String downloadStatus,
                                    @Param("uploadStatus") String uploadStatus);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("""
                UPDATE OutboundFile f
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
