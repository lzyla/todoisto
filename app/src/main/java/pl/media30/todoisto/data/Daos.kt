package pl.media30.todoisto.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, (dueDate IS NULL), dueDate ASC, priority ASC, position ASC, createdAt DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun count(): Int

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND locLat IS NOT NULL")
    suspend fun getWithLocation(): List<Task>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND reminderAt IS NOT NULL")
    suspend fun getWithReminder(): List<Task>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): Task?

    @Query("SELECT * FROM tasks WHERE parentId = :parentId")
    suspend fun getSubtasks(parentId: Long): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Query("UPDATE tasks SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: Long, position: Int)

    @Delete
    suspend fun delete(task: Task)

    @Query("DELETE FROM tasks WHERE id = :id OR parentId = :id")
    suspend fun deleteWithSubtasks(id: Long)

    @Query("DELETE FROM tasks WHERE isCompleted = 1")
    suspend fun deleteCompleted()

    @Query("DELETE FROM tasks WHERE projectId = :projectId")
    suspend fun deleteByProject(projectId: Long)
}

@Dao
interface AreaDao {
    @Query("SELECT * FROM areas ORDER BY position ASC, id ASC")
    fun getAll(): Flow<List<Area>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(area: Area): Long

    @Update
    suspend fun update(area: Area)

    @Query("SELECT COUNT(*) FROM areas")
    suspend fun count(): Int

    @Query("DELETE FROM areas WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY position ASC, id ASC")
    fun getAll(): Flow<List<Project>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: Project): Long

    @Update
    suspend fun update(project: Project)

    @Query("SELECT * FROM projects WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): Project?

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): Project?

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface SectionDao {
    @Query("SELECT * FROM sections ORDER BY position ASC, id ASC")
    fun getAll(): Flow<List<Section>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(section: Section): Long

    @Update
    suspend fun update(section: Section)

    @Query("DELETE FROM sections WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sections WHERE projectId = :projectId")
    suspend fun deleteByProject(projectId: Long)
}

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities ORDER BY isActive DESC, name ASC")
    fun getAll(): Flow<List<Activity>>

    @Query("SELECT * FROM activities WHERE isActive = 1")
    suspend fun getActive(): List<Activity>

    @Query("SELECT * FROM activities WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): Activity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(activity: Activity): Long

    @Update
    suspend fun update(activity: Activity)

    @Query("DELETE FROM activities WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface LabelDao {
    @Query("SELECT * FROM labels ORDER BY name ASC")
    fun getAll(): Flow<List<Label>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(label: Label): Long

    @Update
    suspend fun update(label: Label)

    @Query("SELECT * FROM labels WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): Label?

    @Query("SELECT * FROM labels WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): Label?

    @Query("DELETE FROM labels WHERE id = :id")
    suspend fun deleteById(id: Long)
}
