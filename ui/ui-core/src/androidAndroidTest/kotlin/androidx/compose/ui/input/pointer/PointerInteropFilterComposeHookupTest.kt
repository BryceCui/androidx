/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.ui.input.pointer

import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.DensityAmbient
import androidx.compose.ui.platform.PointerEventPass
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.test.TestActivity
import androidx.test.filters.MediumTest
import androidx.ui.test.android.createAndroidComposeRule
import androidx.ui.test.runOnIdle
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

// Tests basic operations to make sure that pointerInputModifier(view: AndroidViewHolder) is
// hooked up to PointerInteropFilter correctly.
@MediumTest
@RunWith(JUnit4::class)
class PointerInteropFilterComposeHookupTest {

    private lateinit var root: View
    private val motionEventLog = mutableListOf<MotionEvent?>()
    private val eventStringLog = mutableListOf<String>()
    private var retVal = true
    private val motionEventCallback: (MotionEvent?) -> Boolean = {
        motionEventLog.add(it)
        eventStringLog.add("motionEvent")
        retVal
    }
    private val disallowInterceptRequester = RequestDisallowInterceptTouchEvent()

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    @Before
    fun setup() {
        composeTestRule.activityRule.scenario.onActivity { activity ->

            val parent = FrameLayout(activity).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setContent(Recomposer.current()) {
                    with(DensityAmbient.current) {
                        Box(modifier = Modifier
                            .spyGestureFilter {
                                eventStringLog.add(it.name)
                            }
                            .pointerInteropFilter(
                                disallowInterceptRequester,
                                motionEventCallback
                            )
                            .size(100f.toDp(), 100f.toDp())
                        )
                    }
                }
            }

            activity.setContentView(parent)
            root = activity.findViewById(android.R.id.content)
        }
    }

    @Test
    fun ui_down_downMotionEventIsReceived() {
        val down =
            MotionEvent(
                0,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(50f, 50f)),
                root
            )

        runOnIdle {
            root.dispatchTouchEvent(down)
        }

        assertThat(motionEventLog).hasSize(1)
        assertThat(motionEventLog[0]).isSameInstanceAs(down)
    }

    @Test
    fun ui_downUp_upMotionEventIsReceived() {
        val down =
            MotionEvent(
                0,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(50f, 50f)),
                root
            )
        val up =
            MotionEvent(
                10,
                ACTION_UP,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(50f, 50f)),
                root
            )

        runOnIdle {
            root.dispatchTouchEvent(down)
            root.dispatchTouchEvent(up)
        }

        assertThat(motionEventLog).hasSize(2)
        assertThat(motionEventLog[1]).isSameInstanceAs(up)
    }

    @Test
    fun ui_downRetFalseUp_onlyDownIsReceived() {
        retVal = false

        val down =
            MotionEvent(
                0,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(50f, 50f)),
                root
            )
        val up =
            MotionEvent(
                10,
                ACTION_UP,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(50f, 50f)),
                root
            )

        runOnIdle {
            root.dispatchTouchEvent(down)
            root.dispatchTouchEvent(up)
        }

        assertThat(motionEventLog).hasSize(1)
        assertThat(motionEventLog[0]).isSameInstanceAs(down)
    }

    @Test
    fun ui_downRetFalseUpMoveUp_onlyDownIsReceived() {
        retVal = false

        val down =
            MotionEvent(
                0,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(50f, 50f)),
                root
            )
        val move =
            MotionEvent(
                10,
                ACTION_MOVE,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(100f, 50f)),
                root
            )
        val up =
            MotionEvent(
                20,
                ACTION_UP,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(100f, 50f)),
                root
            )

        runOnIdle {
            root.dispatchTouchEvent(down)
            root.dispatchTouchEvent(move)
            root.dispatchTouchEvent(up)
        }

        assertThat(motionEventLog).hasSize(1)
        assertThat(motionEventLog[0]).isSameInstanceAs(down)
    }

    @Test
    fun ui_downRetFalseUpDown_2ndDownIsReceived() {
        retVal = false

        val down =
            MotionEvent(
                0,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(50f, 50f)),
                root
            )
        val up =
            MotionEvent(
                10,
                ACTION_UP,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(50f, 50f)),
                root
            )
        val down2 =
            MotionEvent(
                2,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(50f, 50f)),
                root
            )

        runOnIdle {
            root.dispatchTouchEvent(down)
            root.dispatchTouchEvent(up)
            root.dispatchTouchEvent(down2)
        }

        assertThat(motionEventLog).hasSize(2)
        assertThat(motionEventLog[1]).isSameInstanceAs(down2)
    }

    @Test
    fun ui_downMove_moveIsDispatchedDuringPostDown() {
        val down =
            MotionEvent(
                0,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(50f, 50f)),
                root
            )
        val move =
            MotionEvent(
                10,
                ACTION_MOVE,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(100f, 50f)),
                root
            )

        runOnIdle {
            root.dispatchTouchEvent(down)
            eventStringLog.clear()
            root.dispatchTouchEvent(move)
        }

        assertThat(eventStringLog).hasSize(6)
        assertThat(eventStringLog[0]).isEqualTo(PointerEventPass.InitialDown.toString())
        assertThat(eventStringLog[1]).isEqualTo(PointerEventPass.PreUp.toString())
        assertThat(eventStringLog[2]).isEqualTo(PointerEventPass.PreDown.toString())
        assertThat(eventStringLog[3]).isEqualTo(PointerEventPass.PostUp.toString())
        assertThat(eventStringLog[4]).isEqualTo(PointerEventPass.PostDown.toString())
        assertThat(eventStringLog[5]).isEqualTo("motionEvent")
    }

    @Test
    fun ui_downDisallowInterceptMove_moveIsDispatchedDuringInitialDown() {
        val down =
            MotionEvent(
                0,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(50f, 50f)),
                root
            )
        val move =
            MotionEvent(
                10,
                ACTION_MOVE,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(100f, 50f)),
                root
            )

        runOnIdle {
            root.dispatchTouchEvent(down)
            eventStringLog.clear()
            disallowInterceptRequester.invoke(true)
            root.dispatchTouchEvent(move)
        }

        assertThat(eventStringLog).hasSize(6)
        assertThat(eventStringLog[0]).isEqualTo(PointerEventPass.InitialDown.toString())
        assertThat(eventStringLog[1]).isEqualTo("motionEvent")
        assertThat(eventStringLog[2]).isEqualTo(PointerEventPass.PreUp.toString())
        assertThat(eventStringLog[3]).isEqualTo(PointerEventPass.PreDown.toString())
        assertThat(eventStringLog[4]).isEqualTo(PointerEventPass.PostUp.toString())
        assertThat(eventStringLog[5]).isEqualTo(PointerEventPass.PostDown.toString())
    }

    @Test
    fun ui_downDisallowInterceptMoveAllowInterceptMove_2ndMoveIsDispatchedDuringPostDown() {
        val down =
            MotionEvent(
                0,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(50f, 50f)),
                root
            )
        val move1 =
            MotionEvent(
                10,
                ACTION_MOVE,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(100f, 50f)),
                root
            )
        val move2 =
            MotionEvent(
                10,
                ACTION_MOVE,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(100f, 50f)),
                root
            )

        runOnIdle {
            root.dispatchTouchEvent(down)
            disallowInterceptRequester.invoke(true)
            root.dispatchTouchEvent(move1)
            eventStringLog.clear()
            disallowInterceptRequester.invoke(false)
            root.dispatchTouchEvent(move2)
        }

        assertThat(eventStringLog).hasSize(6)
        assertThat(eventStringLog[0]).isEqualTo(PointerEventPass.InitialDown.toString())
        assertThat(eventStringLog[1]).isEqualTo(PointerEventPass.PreUp.toString())
        assertThat(eventStringLog[2]).isEqualTo(PointerEventPass.PreDown.toString())
        assertThat(eventStringLog[3]).isEqualTo(PointerEventPass.PostUp.toString())
        assertThat(eventStringLog[4]).isEqualTo(PointerEventPass.PostDown.toString())
        assertThat(eventStringLog[5]).isEqualTo("motionEvent")
    }

    @Test
    fun ui_downDisallowInterceptUpDownMove_2ndMoveIsDispatchedDuringPostDown() {
        val down =
            MotionEvent(
                0,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(50f, 50f)),
                root
            )
        val up =
            MotionEvent(
                10,
                ACTION_UP,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(50f, 50f)),
                root
            )
        val downB =
            MotionEvent(
                20,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(50f, 50f)),
                root
            )
        val moveB =
            MotionEvent(
                30,
                ACTION_MOVE,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(100f, 50f)),
                root
            )

        runOnIdle {
            root.dispatchTouchEvent(down)
            disallowInterceptRequester.invoke(true)
            root.dispatchTouchEvent(up)
            root.dispatchTouchEvent(downB)
            eventStringLog.clear()
            root.dispatchTouchEvent(moveB)
        }

        assertThat(eventStringLog).hasSize(6)
        assertThat(eventStringLog[0]).isEqualTo(PointerEventPass.InitialDown.toString())
        assertThat(eventStringLog[1]).isEqualTo(PointerEventPass.PreUp.toString())
        assertThat(eventStringLog[2]).isEqualTo(PointerEventPass.PreDown.toString())
        assertThat(eventStringLog[3]).isEqualTo(PointerEventPass.PostUp.toString())
        assertThat(eventStringLog[4]).isEqualTo(PointerEventPass.PostDown.toString())
        assertThat(eventStringLog[5]).isEqualTo("motionEvent")
    }
}