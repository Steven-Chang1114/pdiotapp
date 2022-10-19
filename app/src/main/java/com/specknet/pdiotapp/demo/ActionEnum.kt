package com.specknet.pdiotapp.demo

enum class ActionEnum(val movement: String) {
    SITTING_STRAIGHT("Sitting straight"),
    SITTING_BENT_FORWARD("Sitting bent forward"),
    SITTING_BENT_BACKWARD("Sitting bent backward"),
    STANDING("Standing"),
    LYING_DOWN_ON_THE_LEFT_SIDE("Lying down on the left side"),
    LYING_DOWN_ON_THE_RIGHT_SIDE("Lying down on the right side"),
    LYING_DOWN_ON_STOMACH("Lying down on stomach"),
    LYING_DOWN_ON_THE_BACK("Lying down on the back"),
    WALKING_AT_NORMAL_SPEED("Walking at Normal Speed"),
    RUNNING("Running or Jogging"),
    ASCENDING_STAIRS("Ascending stairs"),
    DESCENDING_STAIRS("Descending stairs"),
    DESK_WORK("Desk work"),
    MOVEMENT("General movement"),
    LOADING("Loading")
}