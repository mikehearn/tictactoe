import Player.O
import Player.X
import javafx.application.Application
import javafx.beans.Observable
import javafx.beans.binding.Bindings.createObjectBinding
import javafx.beans.binding.ObjectBinding
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.Stage
import java.util.concurrent.Callable

enum class Player { X, O }

fun Player?.label() = when(this) {
    X -> "X"; O -> "O"; null -> ""
}

val lines: List<Triple<Int, Int, Int>> = listOf(
        Triple(0, 1, 2),
        Triple(3, 4, 5),
        Triple(6, 7, 8),
        Triple(0, 3, 6),
        Triple(1, 4, 7),
        Triple(2, 5, 8),
        Triple(0, 4, 8),
        Triple(2, 4, 6)
)

class Board(private val squares: Array<Player?> = Array(9) { null }) {
    private fun coord(x: Int, y: Int) = x + (y * 3)
    operator fun get(x: Int, y: Int): Player? = squares[coord(x, y)]
    fun with(x: Int, y: Int, content: Player?) = Board(squares.copyOf()).also { it.squares[coord(x, y)] = content }

    fun computeWinner(): Player? {
        for ((a, b, c) in lines) {
            if (squares[a] != null && squares[a] == squares[b] && squares[b] == squares[c])
                return squares[a]
        }
        return null
    }
}

fun <T, R> ObservableList<T>.compute(block: (ObservableList<T>) -> R): ObjectBinding<R> = createObjectBinding(Callable { block(this) }, this)
fun <T, R> ObservableList<T>.computeLast(block: (T) -> R) = compute { block(it.last()) }

class TicTacToe : Application() {
    private val boards = FXCollections.observableArrayList<Board>()
    private var currentPlayer = X

    override fun start(stage: Stage) {
        val gameMoves = VBox().also { it.styleClass += "game-moves" }
        boards.addListener { _: Observable ->
            gameMoves.children.setAll(boards.indices.map { index ->
                val i = index + 1
                Button("$i. Go to move $i").also { it.setOnAction { jumpTo(i) } }
            })
        }

        boards += Board()
        val statusLabel = Label().also { it.styleClass += "status-label" }
        statusLabel.textProperty().bind(boards.compute { statusText() })

        val hbox = HBox(
                createGrid(), VBox(
                    statusLabel,
                    gameMoves
                )
        )
        hbox.styleClass += "game"
        with(stage) {
            scene = Scene(hbox)
            scene.stylesheets.add("tictactoe.css")
            title = "Tic Tac Toe"
            show()
        }
    }

        private fun jumpTo(newIndex: Int) {
            currentPlayer = if (newIndex - 1 % 2 == 0) X else O
            boards.setAll(boards.take(newIndex))
        }

    private fun statusText(): String {
        val winner = boards.last().computeWinner()
        return when {
            winner == X -> "X is the winner!"
            winner == O -> "O is the winner!"
            currentPlayer == X -> "Current player: X"
            currentPlayer == O -> "Current player: O"
            else -> error("Unreachable")
        }
    }

    private fun createGrid(): GridPane {
        val grid = GridPane()
        grid.styleClass.add("board")
        for (x in 0..2) {
            for (y in 0..2) {
                val button = Button()
                button.textProperty().bind(boards.computeLast { it[x, y].label() })
                button.disableProperty().bind(boards.computeLast { it.computeWinner() != null || it[x, y] != null })
                button.setOnAction { playMove(x, y) }
                grid.add(button, x, y)
            }
        }
        return grid
    }

    private fun playMove(x: Int, y: Int) {
        val currentBoard = boards.last()
        check(currentBoard[x, y] == null)
        val player: Player = currentPlayer
        currentPlayer = when(player) {
            X -> O
            O -> X
        }
        boards.add(currentBoard.with(x, y, player))
    }
}

fun main(args: Array<String>) {
    Application.launch(TicTacToe::class.java, *args)
}