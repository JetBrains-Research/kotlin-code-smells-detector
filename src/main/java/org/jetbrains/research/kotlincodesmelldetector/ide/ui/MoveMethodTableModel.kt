package org.jetbrains.research.kotlincodesmelldetector.ide.ui

import com.intellij.ui.BooleanTableCellRenderer
import com.intellij.ui.JBColor
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.research.kotlincodesmelldetector.KotlinCodeSmellDetectorBundle
import org.jetbrains.research.kotlincodesmelldetector.ide.refactoring.moveMethod.MoveMethodRefactoring
import org.jetbrains.research.kotlincodesmelldetector.utils.signature
import java.awt.Component
import java.util.*
import java.util.function.Consumer
import java.util.stream.Collectors
import java.util.stream.IntStream
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer

internal class MoveMethodTableModel(refactorings: List<MoveMethodRefactoring>) : AbstractTableModel() {
    private val refactorings: MutableList<MoveMethodRefactoring> = ArrayList()
    private val virtualRows: MutableList<Int> = ArrayList()
    private lateinit var isSelected: BooleanArray
    fun updateTable(refactorings: List<MoveMethodRefactoring>) {
        this.refactorings.clear()
        this.refactorings.addAll(refactorings)
        isSelected = BooleanArray(refactorings.size)
        IntStream.range(0, refactorings.size).forEachOrdered { e: Int -> virtualRows.add(e) }
        fireTableDataChanged()
    }

    fun clearTable() {
        refactorings.clear()
        virtualRows.clear()
        isSelected = BooleanArray(0)
        fireTableDataChanged()
    }

    fun selectAll() {
        for (i in virtualRows.indices) {
            setValueAtRowIndex(true, i, false)
        }
        fireTableDataChanged()
    }

    fun deselectAll() {
        for (i in virtualRows.indices) {
            setValueAtRowIndex(false, i, false)
        }
        fireTableDataChanged()
    }

    fun updateRows() {
        virtualRows.forEach(Consumer { i: Int? ->
            if (!refactorings[i!!].optionalMethod.isPresent) {
                isSelected[i] = false
            }
        })
        fireTableDataChanged()
    }

    fun pullSelected(): List<MoveMethodRefactoring> {
        return virtualRows.stream()
                .filter { i: Int? -> isSelected[i!!] && refactorings[i].optionalMethod.isPresent }
                .map { index: Int? -> refactorings[index!!] }
                .collect(Collectors.toList())
    }

    override fun getColumnCount(): Int {
        return COLUMNS_COUNT
    }

    override fun getColumnName(column: Int): String {
        when (column) {
            SELECTION_COLUMN_INDEX -> return ""
            ENTITY_COLUMN_INDEX -> return KotlinCodeSmellDetectorBundle.message("method.column.title")
            MOVE_TO_COLUMN_INDEX -> return KotlinCodeSmellDetectorBundle.message("move.to.column.title")
            ACCESSED_MEMBERS_COUNT_INDEX -> return KotlinCodeSmellDetectorBundle.message("dependencies.column.title")
        }
        throw IndexOutOfBoundsException("Unexpected column index: $column")
    }

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
        return columnIndex == SELECTION_COLUMN_INDEX && refactorings[rowIndex].optionalMethod.isPresent
    }

    override fun getColumnClass(columnIndex: Int): Class<*> {
        return if (columnIndex == SELECTION_COLUMN_INDEX) Boolean::class.java else String::class.java
    }

    override fun getRowCount(): Int {
        return virtualRows.size
    }

    override fun setValueAt(value: Any, virtualRow: Int, columnIndex: Int) {
        val rowIndex = virtualRows[virtualRow]
        val isRowSelected = value as Boolean
        setValueAtRowIndex(isRowSelected, rowIndex, true)
        fireTableDataChanged()
    }

    private fun setValueAtRowIndex(isRowSelected: Boolean, rowIndex: Int, forceSelectInConflicts: Boolean) {
        if (!refactorings[rowIndex].optionalMethod.isPresent) {
            return
        }
        isSelected[rowIndex] = isRowSelected
    }

    override fun getValueAt(virtualRow: Int, columnIndex: Int): Any? {
        val rowIndex = virtualRows[virtualRow]
        when (columnIndex) {
            SELECTION_COLUMN_INDEX -> return isSelected[rowIndex]
            ENTITY_COLUMN_INDEX -> {
                val method = refactorings[rowIndex].optionalMethod
                val qualifiedMethodName = refactorings[rowIndex].qualifiedMethodName
                return method.map { qualifiedMethodName }.orElseGet {
                    (qualifiedMethodName + " | " + KotlinCodeSmellDetectorBundle.message("kotlin.member.is.not.valid"))
                }
            }
            MOVE_TO_COLUMN_INDEX -> {
                val targetClass = refactorings[rowIndex].optionalTargetClass
                return targetClass.map { ktClassOrObject: KtClassOrObject -> ktClassOrObject.signature }.orElseGet { KotlinCodeSmellDetectorBundle.message("target.class.is.not.valid") }
            }
            ACCESSED_MEMBERS_COUNT_INDEX -> return refactorings[rowIndex].sourceAccessedMembers.toString() + "/" + refactorings[rowIndex].targetAccessedMembers
        }
        throw IndexOutOfBoundsException("Unexpected column index: $columnIndex")
    }

    fun getUnitAt(virtualRow: Int, column: Int): Optional<out KtElement?> {
        val row = virtualRows[virtualRow]
        when (column) {
            ENTITY_COLUMN_INDEX -> return refactorings[row].optionalMethod
            MOVE_TO_COLUMN_INDEX -> return refactorings[row].optionalTargetClass
        }
        throw IndexOutOfBoundsException("Unexpected column index: $column")
    }

    fun setupRenderer(table: JTable) {
        table.setDefaultRenderer(Boolean::class.java, object : BooleanTableCellRenderer() {
            private val EMPTY_LABEL = JLabel()
            override fun getTableCellRendererComponent(table: JTable, value: Any, isSel: Boolean, hasFocus: Boolean,
                                                       row: Int, column: Int): Component {
                val realRow = virtualRows[table.convertRowIndexToModel(row)]
                return if (refactorings[realRow].optionalMethod.isPresent) {
                    super.getTableCellRendererComponent(table, value, isSel, hasFocus, row, column)
                } else {
                    EMPTY_LABEL
                }
            }

            init {
                EMPTY_LABEL.background = JBColor.LIGHT_GRAY
                EMPTY_LABEL.isOpaque = true
            }
        })
        table.setDefaultRenderer(String::class.java, object : DefaultTableCellRenderer() {
            override fun getTableCellRendererComponent(table: JTable, value: Any, isSelected: Boolean,
                                                       hasFocus: Boolean, virtualRow: Int, column: Int): Component {
                val row = virtualRows[table.convertRowIndexToModel(virtualRow)]
                background = if (!refactorings[row].optionalMethod.isPresent) {
                    JBColor.LIGHT_GRAY
                } else if (isSelected) {
                    table.selectionBackground
                } else {
                    table.background
                }
                isEnabled = refactorings[row].optionalMethod.isPresent
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, virtualRow, column)
            }
        })
    }

    companion object {
        const val SELECTION_COLUMN_INDEX = 0
        private const val ENTITY_COLUMN_INDEX = 1
        private const val MOVE_TO_COLUMN_INDEX = 2
        private const val ACCESSED_MEMBERS_COUNT_INDEX = 3
        private const val COLUMNS_COUNT = 4
    }

    init {
        updateTable(refactorings)
    }
}