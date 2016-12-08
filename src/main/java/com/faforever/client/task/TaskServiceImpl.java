package com.faforever.client.task;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.ThreadPoolExecutor;

@Lazy
@Service
public class TaskServiceImpl implements TaskService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final ObservableList<Task<?>> activeTasks;

  @Inject
  ThreadPoolExecutor threadPoolExecutor;

  private ObservableList<Task<?>> unmodifiableObservableList;

  public TaskServiceImpl() {
    activeTasks = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
    unmodifiableObservableList = FXCollections.unmodifiableObservableList(activeTasks);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends CompletableTask> T submitTask(T task) {
    task.getFuture().whenComplete((o, throwable) -> {
      activeTasks.remove(task);
      if (throwable != null) {
        logger.warn("Task failed", (Throwable) throwable);
      }
    });

    activeTasks.add(task);
    threadPoolExecutor.execute(task);

    return task;
  }

  @Override
  public ObservableList<Task<?>> getActiveTasks() {
    return unmodifiableObservableList;
  }

  @PreDestroy
  void preDestroy() {
    threadPoolExecutor.shutdownNow();
  }
}
