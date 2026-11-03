package pl.media30.todoisto.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import pl.media30.todoisto.shared.Ids

/*
 * Konwencja DAO (od wersji bazy 9):
 *  - `insert` / `update` to zapisy WYKONANE NA TYM URZĄDZENIU: nadają globalne id
 *    (gdy id == 0) i stemplują `updatedAt`, żeby scalanie „nowszy wygrywa" działało.
 *  - `upsertRaw` to zapis 1:1 (bez zmiany id i znaczników) — tylko dla danych
 *    przychodzących z synchronizacji / kopii, które już mają swoje znaczniki.
 * Dzięki temu żadna ścieżka (UI, seeder, import CSV) nie może przypadkiem nadać
 * id z autoinkrementu ani zgubić znacznika zmiany.
 */
private fun now() = System.currentTimeMillis()
private fun idOr(id: Long) = if (id == 0L) Ids.next() else id

@Dao
abstract class TaskDao {

    @Query("SELECT * FROM tasks WHERE deleted = 0 ORDER BY isCompleted ASC, (dueDate IS NULL), dueDate ASC, priority ASC, position ASC, createdAt DESC")
    abstract fun getAllTasks(): Flow<List<Task>>

    /** Wszystkie zadania łącznie z nagrobkami — do kopii/synchronizacji. */
    @Query("SELECT * FROM tasks")
    abstract suspend fun getAllRaw(): List<Task>

    @Query("SELECT COUNT(*) FROM tasks WHERE deleted = 0")
    abstract suspend fun count(): Int

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND deleted = 0 AND locLat IS NOT NULL")
    abstract suspend fun getWithLocation(): List<Task>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND deleted = 0 AND reminderAt IS NOT NULL")
    abstract suspend fun getWithReminder(): List<Task>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    abstract suspend fun getTaskById(id: Long): Task?

    @Query("SELECT * FROM tasks WHERE parentId = :parentId")
    abstract suspend fun getSubtasks(parentId: Long): List<Task>

    /** Zadania czekające na przekazanie do narzędzia projektu (Notion). */
    @Query("SELECT * FROM tasks WHERE routeState = 'pending' AND deleted = 0")
    abstract suspend fun getPendingRoutes(): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertRaw(task: Task): Long

    @Update
    protected abstract suspend fun updateRaw(task: Task)

    open suspend fun insert(task: Task): Long {
        val t = now()
        // Nowe zadanie (np. duplikat) nie dziedziczy powiązania z kopią w Notion.
        val fresh = if (task.id == 0L) task.copy(remoteUrl = null, routeState = task.routeState.takeIf { it == "pending" }) else task
        return upsertRaw(fresh.copy(id = idOr(task.id), createdAt = if (task.createdAt == 0L) t else task.createdAt, updatedAt = t))
    }

    open suspend fun update(task: Task) = updateRaw(task.copy(updatedAt = now()))

    /** Zmiana stanu przekazania nie jest edycją treści — nie rusza `updatedAt`. */
    @Query("UPDATE tasks SET routeState = :state, remoteUrl = COALESCE(:remoteUrl, remoteUrl) WHERE id = :id")
    abstract suspend fun setRouteState(id: Long, state: String?, remoteUrl: String?)

    @Query("UPDATE tasks SET position = :position, updatedAt = :now WHERE id = :id")
    protected abstract suspend fun updatePositionAt(id: Long, position: Int, now: Long)

    open suspend fun updatePosition(id: Long, position: Int) = updatePositionAt(id, position, now())

    @Delete
    abstract suspend fun delete(task: Task)

    /** Miękkie usunięcie (nagrobek) — usunięcie propaguje się przy synchronizacji. */
    @Query("UPDATE tasks SET deleted = 1, updatedAt = :now WHERE id = :id OR parentId = :id")
    abstract suspend fun softDeleteWithSubtasks(id: Long, now: Long)

    /** Usuwa ukończone jako nagrobki (twarde DELETE wracałoby z drugiego urządzenia). */
    @Query("UPDATE tasks SET deleted = 1, updatedAt = :now WHERE isCompleted = 1 AND deleted = 0")
    protected abstract suspend fun softDeleteCompletedAt(now: Long)

    open suspend fun deleteCompleted() = softDeleteCompletedAt(now())

    /** Czyści stare nagrobki (>30 dni), żeby baza nie puchła. */
    @Query("DELETE FROM tasks WHERE deleted = 1 AND updatedAt > 0 AND updatedAt < :cutoff")
    abstract suspend fun purgeOldTombstones(cutoff: Long)

    @Query("UPDATE tasks SET deleted = 1, updatedAt = :now WHERE projectId = :projectId AND deleted = 0")
    protected abstract suspend fun softDeleteByProjectAt(projectId: Long, now: Long)

    open suspend fun deleteByProject(projectId: Long) = softDeleteByProjectAt(projectId, now())
}

@Dao
abstract class AreaDao {
    @Query("SELECT * FROM areas ORDER BY position ASC, id ASC")
    abstract fun getAll(): Flow<List<Area>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertRaw(area: Area): Long

    @Update
    protected abstract suspend fun updateRaw(area: Area)

    open suspend fun insert(area: Area): Long = upsertRaw(area.copy(id = idOr(area.id), updatedAt = now()))
    open suspend fun update(area: Area) = updateRaw(area.copy(updatedAt = now()))

    @Query("SELECT COUNT(*) FROM areas")
    abstract suspend fun count(): Int

    @Query("DELETE FROM areas WHERE id = :id")
    abstract suspend fun deleteById(id: Long)
}

@Dao
abstract class ProjectDao {
    @Query("SELECT * FROM projects ORDER BY position ASC, id ASC")
    abstract fun getAll(): Flow<List<Project>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertRaw(project: Project): Long

    @Update
    protected abstract suspend fun updateRaw(project: Project)

    open suspend fun insert(project: Project): Long = upsertRaw(project.copy(id = idOr(project.id), updatedAt = now()))
    open suspend fun update(project: Project) = updateRaw(project.copy(updatedAt = now()))

    @Query("SELECT * FROM projects WHERE name = :name LIMIT 1")
    abstract suspend fun findByName(name: String): Project?

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    abstract suspend fun findById(id: Long): Project?

    @Query("DELETE FROM projects WHERE id = :id")
    abstract suspend fun deleteById(id: Long)
}

@Dao
abstract class SectionDao {
    @Query("SELECT * FROM sections ORDER BY position ASC, id ASC")
    abstract fun getAll(): Flow<List<Section>>

    @Query("SELECT id FROM sections WHERE projectId = :projectId")
    abstract suspend fun idsByProject(projectId: Long): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertRaw(section: Section): Long

    @Update
    protected abstract suspend fun updateRaw(section: Section)

    open suspend fun insert(section: Section): Long = upsertRaw(section.copy(id = idOr(section.id), updatedAt = now()))
    open suspend fun update(section: Section) = updateRaw(section.copy(updatedAt = now()))

    @Query("DELETE FROM sections WHERE id = :id")
    abstract suspend fun deleteById(id: Long)

    @Query("DELETE FROM sections WHERE projectId = :projectId")
    abstract suspend fun deleteByProject(projectId: Long)
}

@Dao
abstract class ActivityDao {
    @Query("SELECT * FROM activities ORDER BY isActive DESC, name ASC")
    abstract fun getAll(): Flow<List<Activity>>

    @Query("SELECT * FROM activities WHERE isActive = 1")
    abstract suspend fun getActive(): List<Activity>

    @Query("SELECT * FROM activities WHERE id = :id LIMIT 1")
    abstract suspend fun findById(id: Long): Activity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertRaw(activity: Activity): Long

    @Update
    protected abstract suspend fun updateRaw(activity: Activity)

    open suspend fun insert(activity: Activity): Long {
        val t = now()
        return upsertRaw(activity.copy(id = idOr(activity.id), createdAt = if (activity.createdAt == 0L) t else activity.createdAt, updatedAt = t))
    }
    open suspend fun update(activity: Activity) = updateRaw(activity.copy(updatedAt = now()))

    @Query("DELETE FROM activities WHERE id = :id")
    abstract suspend fun deleteById(id: Long)
}

@Dao
abstract class LabelDao {
    @Query("SELECT * FROM labels ORDER BY name ASC")
    abstract fun getAll(): Flow<List<Label>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertRaw(label: Label): Long

    @Update
    protected abstract suspend fun updateRaw(label: Label)

    open suspend fun insert(label: Label): Long = upsertRaw(label.copy(id = idOr(label.id), updatedAt = now()))
    open suspend fun update(label: Label) = updateRaw(label.copy(updatedAt = now()))

    @Query("SELECT * FROM labels WHERE name = :name LIMIT 1")
    abstract suspend fun findByName(name: String): Label?

    @Query("SELECT * FROM labels WHERE id = :id LIMIT 1")
    abstract suspend fun findById(id: Long): Label?

    @Query("DELETE FROM labels WHERE id = :id")
    abstract suspend fun deleteById(id: Long)
}

@Dao
abstract class TombstoneDao {
    @Query("SELECT * FROM tombstones")
    abstract suspend fun getAll(): List<Tombstone>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(t: Tombstone)

    @Query("DELETE FROM tombstones WHERE deletedAt < :cutoff")
    abstract suspend fun purgeOlderThan(cutoff: Long)

    open suspend fun record(kind: String, id: Long) = upsert(Tombstone(kind, id, now()))
}
