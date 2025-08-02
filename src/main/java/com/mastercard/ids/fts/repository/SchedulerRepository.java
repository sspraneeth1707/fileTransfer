package com.mastercard.ids.fts.repository;

import com.mastercard.ids.fts.model.SchedulerLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SchedulerRepository extends JpaRepository<SchedulerLog, String> {
}
