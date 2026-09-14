package com.sih26001.mobilealert.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class DatabaseMigrationUnitTest {

    @Test
    fun `migration 2 to 3 executes expected ALTER TABLE statements with INTEGER for timestamps`() {
        val mockDb = mock(SupportSQLiteDatabase::class.java)

        AppDatabase.MIGRATION_2_3.migrate(mockDb)

        verify(mockDb).execSQL("ALTER TABLE pending_acks ADD COLUMN lastFailureAt INTEGER")
        verify(mockDb).execSQL("ALTER TABLE pending_acks ADD COLUMN lastFailureMessage TEXT")
        verify(mockDb).execSQL("ALTER TABLE pending_acks ADD COLUMN completedAt INTEGER")
    }
}
