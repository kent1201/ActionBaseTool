# ActionBaseTool

本專案與資策會合作將深度學習 Pytorch 與 android app 結合並進行開發應用。在深度學習方面對人體下肢動作辨識集進行訓練建立分類模型，並將訓練好的分類模型結合進手機後，便可使用手機配戴感測器來進行下肢動作的辨識。

## Introduction

本專案讓使用者在手腕上配戴感測器並通過藍芽連接手機，將資料回傳至手機上，來分辨使用者的動作。感測器透過接收使用者特定動作並建立相對應的資料集(詳見: [TimeGAN-Pytorch](https://github.com/kent1201/TimeGAN-Pytorch)，內有資料集說明)。將收集好的資料集回傳至電腦，使用深度學習對人體下肢動作辨識資料集進行訓練並建立分類模型，詳細流程可至 [Representing Deep Motion Bases for Sensor Action Recognition](https://hdl.handle.net/11296/726654)觀看。再將訓練好的分類模型移植至手機並並以此開發app來進行應用。本專案最後可以在手機上使用該 app 搭配 cavy sensor，接收資料並即時顯示使用者的資料。

## Project architecture


