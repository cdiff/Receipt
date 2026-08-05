package com.pasic.receipt.ui.home

import com.pasic.receipt.data.local.entity.ReceiptEntity
import com.pasic.receipt.data.repository.ReceiptRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val repository: ReceiptRepository = Mockito.mock(ReceiptRepository::class.java)
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `DB가 비어있을 때 기본 시연용 데모 영수증 2건을 제공한다`() = runTest {
        val emptyFlow = flowOf<List<ReceiptEntity>>(emptyList())
        Mockito.doReturn(emptyFlow).`when`(repository).getAllReceipts()

        val viewModel = HomeViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(5, state.recentReceipts.size)
        assertEquals("스타벅스 강남대로점", state.recentReceipts[0].merchantName)
        assertEquals("CU 역삼하이츠점", state.recentReceipts[1].merchantName)
    }

    @Test
    fun `DB에 영수증 데이터가 존재하면 해당 영수증 목록을 매핑한다`() = runTest {
        val testList = listOf(
            ReceiptEntity(
                id = 10,
                merchantName = "투썸플레이스",
                date = "2026.07.31",
                totalAmount = 6500.0,
                category = "식비"
            )
        )
        val listFlow = flowOf(testList)
        Mockito.doReturn(listFlow).`when`(repository).getAllReceipts()

        val viewModel = HomeViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.recentReceipts.size)
        assertEquals("투썸플레이스", state.recentReceipts[0].merchantName)
    }
}
