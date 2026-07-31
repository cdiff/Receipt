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
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val repository: ReceiptRepository = mock(ReceiptRepository::class.java)
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
    fun `loadData provides default mock receipts when database is empty`() = runTest {
        `when`(repository.getAllReceipts()).thenReturn(flowOf(emptyList()))

        val viewModel = HomeViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.recentReceipts.size)
        assertEquals("스타벅스 강남점", state.recentReceipts[0].merchantName)
        assertEquals("명동교자 본점", state.recentReceipts[1].merchantName)
    }

    @Test
    fun `loadData maps receipts from repository when available`() = runTest {
        val testList = listOf(
            ReceiptEntity(
                id = 10,
                merchantName = "투썸플레이스",
                date = "2026.07.31",
                totalAmount = 6500.0,
                category = "식비"
            )
        )
        `when`(repository.getAllReceipts()).thenReturn(flowOf(testList))

        val viewModel = HomeViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.recentReceipts.size)
        assertEquals("투썸플레이스", state.recentReceipts[0].merchantName)
    }
}
