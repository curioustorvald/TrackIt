package net.torvald.trackit

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarumsansbitmap.gdx.GameFontBase

object TaskMain : Screen {

    private lateinit var targetTex: Texture
    private lateinit var playerTex: Texture

    private lateinit var batch: SpriteBatch
    private lateinit var shapeRenderer: ShapeRenderer


    var targetPos = Gdx.graphics.width / 2f // could also make a fun game if the target also moves
    var playerPos = Gdx.graphics.width / 2f

    private val runtime = 30f // seconds


    private val displaceMax = 60
    private val minimalDisplacement = 40
    private val disturbancePoints = FloatArray(5)
    // cosine interpolation, ensure 2 negs and 2 pos
    // displace PLAYER by reference-value-of-current-frame minus reference-value-of-prev-frame
    private val disturbanceInterval = runtime / disturbancePoints.lastIndex

    private var prevDisturbance = 0f // used for doing discrete differentiation


    private val dataPointCount = 200

    private val pollingTime = runtime / dataPointCount

    private val dataPoints = ArrayList<ControlDataPoints>()

    private var runtimeCounter = 0f
    private var dataCaptureCounter = 0f

    private var startGame = false

    lateinit var font: GameFontBase

    init {
        for (i in 0..3) { // will generate [pos, neg, pos, neg]
            disturbancePoints[i] = Math.random().toFloat() * displaceMax + if (i % 2 == 0) minimalDisplacement else -minimalDisplacement
        }


        // for 50 % chance, make it [neg, pos, neg, pos]
        if (Math.random() < 0.5) {
            disturbancePoints.reverse()
        }
    }


    override fun hide() {
        dispose()
    }

    override fun show() {
        batch = SpriteBatch()
        shapeRenderer = ShapeRenderer()

        Gdx.input.inputProcessor = TrackItInputListener()

        targetTex = Texture(Gdx.files.internal("assets/target.tga"))
        playerTex = Texture(Gdx.files.internal("assets/player.tga"))

        font = GameFontBase("assets/fonts")
    }

    private var rmseCalculated = false
    private var rmse = 0f

    override fun render(delta: Float) {
        if (startGame) {
            Gdx.gl.glClearColor(.933f, .933f, .933f, 1f)
        }
        else {
            Gdx.gl.glClearColor(.235f, .235f, .235f, 1f)
        }
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        Gdx.graphics.setTitle("Track It! — FPS: ${Gdx.graphics.framesPerSecond}")


        // TODO start when START is pressed, which is placed at the centre of the screen so that players would be able to align

        if (!startGame) {
            batch.inUse {
                batch.color = Color.WHITE
                val textWidth = font.getWidth(Lang["MENU_LABEL_PLAY"])
                font.draw(batch, Lang["MENU_LABEL_PLAY"], (Gdx.graphics.width - textWidth) / 2f, Gdx.graphics.height / 2f)
            }
        }


        // TASK MODE
        if (runtimeCounter < runtime && startGame) {


            batch.inUse {
                batch.draw(targetTex, targetPos.toInt().toFloat(), (Gdx.graphics.height + targetTex.height) / 2f)
                batch.draw(playerTex, playerPos.toInt().toFloat(), (Gdx.graphics.height - targetTex.height) / 2f)
            }

            val newDisturbance = interpolateCosine(
                    (runtimeCounter % disturbanceInterval) / disturbanceInterval,
                    disturbancePoints[(runtimeCounter / disturbanceInterval).toInt()],
                    disturbancePoints[minOf((runtimeCounter / disturbanceInterval).toInt() + 1, disturbancePoints.lastIndex)]
            )
            playerPos += newDisturbance - prevDisturbance

            prevDisturbance = newDisturbance


            if (dataCaptureCounter > pollingTime) {
                dataCaptureCounter -= pollingTime
                dataPoints.add(ControlDataPoints(
                        newDisturbance + targetPos,
                        playerPos,
                        targetPos
                ))
            }


            runtimeCounter += delta
            dataCaptureCounter += delta
        }
        // RESULT MODE
        else if (startGame) {

            val dataPointCentre = Gdx.graphics.height / 2f
            val pointMargin = 3f
            val pointSize = 2f
            val leftMargin = 8f

            if (!rmseCalculated) {
                var errorSum = 0.0
                dataPoints.forEach { errorSum += Math.pow(it.movement.toDouble() - it.referenceValue, 2.0) }

                rmse = Math.sqrt(errorSum / dataPoints.size).toFloat()

                rmseCalculated = true
            }


            // draw graphs
            shapeRenderer.inUse {

                shapeRenderer.color = Color(0x404040ff)
                // assumes fixed reference position
                shapeRenderer.rect(leftMargin,  dataPointCentre, pointMargin * dataPoints.size, 1f)


                dataPoints.forEachIndexed { index, controlDataPoints ->
                    val normMovement = controlDataPoints.movement - (Gdx.graphics.width / 2)
                    val normDisturbance = controlDataPoints.disturbance - (Gdx.graphics.width / 2)

                    shapeRenderer.color = Color.RED
                    shapeRenderer.rect(leftMargin + index * pointMargin, normMovement + dataPointCentre, pointSize, pointSize)

                    shapeRenderer.color = Color(0x404040ff)
                    shapeRenderer.rect(leftMargin + index * pointMargin, normDisturbance + dataPointCentre, pointSize, pointSize)

                    shapeRenderer.color = Color(0x00cc50ff)
                    shapeRenderer.rect(leftMargin + index * pointMargin, (normDisturbance + normMovement) + dataPointCentre, pointSize, pointSize)

                }
            }


            batch.inUse {
                batch.color = Color(0x00ccffff)
                font.draw(batch, "RMS Error: $rmse", 10f, 10f)
            }

        }
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun resize(p0: Int, p1: Int) {
    }

    override fun dispose() {
        targetTex.dispose()
        playerTex.dispose()
    }


    private fun interpolateCosine(scale: Float, startValue: Float, endValue: Float): Float {
        val ft = scale * Math.PI
        val f = (1 - Math.cos(ft)) * 0.5f

        return (startValue * (1 - f) + endValue * f).toFloat()
    }



    class TrackItInputListener : InputProcessor {

        private var oldMouseX = -100

        override fun touchUp(p0: Int, p1: Int, p2: Int, p3: Int): Boolean {
            return false
        }

        override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
            if (startGame) {
                if (oldMouseX == -100) oldMouseX = screenX // init, -100 is an placeholder value

                playerPos -= (oldMouseX - screenX).toFloat()

                oldMouseX = screenX
            }

            return true
        }

        override fun keyTyped(p0: Char): Boolean {
            return false
        }

        override fun scrolled(p0: Int): Boolean {
            return false
        }

        override fun keyUp(p0: Int): Boolean {
            return false
        }

        override fun touchDragged(p0: Int, p1: Int, p2: Int): Boolean {
            return false
        }

        override fun keyDown(p0: Int): Boolean {
            return false
        }

        override fun touchDown(p0: Int, p1: Int, p2: Int, p3: Int): Boolean {
            if (!startGame) startGame = true

            return true
        }
    }
}


inline fun SpriteBatch.inUse(action: (SpriteBatch) -> Unit) {
    this.begin()
    action.invoke(this)
    this.end()
}

inline fun ShapeRenderer.inUse(action: (ShapeRenderer) -> Unit) {
    this.begin(ShapeRenderer.ShapeType.Filled)
    action.invoke(this)
    this.end()
}

data class ControlDataPoints(val disturbance: Float, val movement: Float, val referenceValue: Float)