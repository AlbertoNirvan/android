package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.database.entity.ActiveTransferEntity
import mega.privacy.android.data.database.entity.ActiveTransferTotalsEntity
import mega.privacy.android.domain.entity.transfer.TransferType

@Dao
internal interface ActiveTransferDao {

    @Query("SELECT * FROM active_transfers WHERE tag = :tag")
    suspend fun getActiveTransferByTag(tag: Int): ActiveTransferEntity?

    @Query("SELECT * FROM active_transfers WHERE transfer_type = :transferType")
    fun getActiveTransfersByType(transferType: TransferType): Flow<List<ActiveTransferEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateActiveTransfer(entity: ActiveTransferEntity)

    @Query("DELETE FROM active_transfers")
    suspend fun deleteAllActiveTransfers()

    @Query("DELETE FROM active_transfers WHERE tag = :tag")
    suspend fun deleteActiveTransferByTag(tag: Int)

    @Query("SELECT transfer_type as transfersType, SUM(total_bytes) as totalBytes, SUM(transferred_bytes) as transferredBytes, COUNT(*) as totalTransfers, SUM(is_finished) as totalFinishedTransfers FROM active_transfers WHERE transfer_type = :transferType GROUP by transfer_type")
    fun getTotalsByType(transferType: TransferType): Flow<ActiveTransferTotalsEntity>

}