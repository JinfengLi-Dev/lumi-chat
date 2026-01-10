/**
 * Audio Visualizer utility for voice recording
 * Uses Web Audio API to create waveform visualization
 */

export interface AudioVisualizerOptions {
  canvas: HTMLCanvasElement
  fftSize?: number
  barWidth?: number
  barGap?: number
  barColor?: string
  backgroundColor?: string
}

export class AudioVisualizer {
  private audioContext: AudioContext | null = null
  private analyser: AnalyserNode | null = null
  private source: MediaStreamAudioSourceNode | null = null
  private canvas: HTMLCanvasElement
  private canvasCtx: CanvasRenderingContext2D
  private animationId: number | null = null
  private dataArray: Uint8Array | null = null

  private readonly fftSize: number
  private readonly barWidth: number
  private readonly barGap: number
  private readonly barColor: string
  private readonly backgroundColor: string

  constructor(options: AudioVisualizerOptions) {
    this.canvas = options.canvas
    const ctx = this.canvas.getContext('2d')
    if (!ctx) {
      throw new Error('Failed to get canvas 2d context')
    }
    this.canvasCtx = ctx

    this.fftSize = options.fftSize || 256
    this.barWidth = options.barWidth || 3
    this.barGap = options.barGap || 1
    this.barColor = options.barColor || '#409eff'
    this.backgroundColor = options.backgroundColor || '#f5f7fa'
  }

  async connect(stream: MediaStream): Promise<void> {
    this.audioContext = new AudioContext()
    this.analyser = this.audioContext.createAnalyser()
    this.analyser.fftSize = this.fftSize

    this.source = this.audioContext.createMediaStreamSource(stream)
    this.source.connect(this.analyser)

    const bufferLength = this.analyser.frequencyBinCount
    this.dataArray = new Uint8Array(bufferLength)

    this.draw()
  }

  private draw = (): void => {
    if (!this.analyser || !this.dataArray) return

    this.animationId = requestAnimationFrame(this.draw)

    this.analyser.getByteFrequencyData(this.dataArray)

    const width = this.canvas.width
    const height = this.canvas.height

    // Clear canvas
    this.canvasCtx.fillStyle = this.backgroundColor
    this.canvasCtx.fillRect(0, 0, width, height)

    // Calculate number of bars that fit
    const totalBarWidth = this.barWidth + this.barGap
    const numBars = Math.floor(width / totalBarWidth)
    const step = Math.floor(this.dataArray.length / numBars)

    this.canvasCtx.fillStyle = this.barColor

    for (let i = 0; i < numBars; i++) {
      // Sample the frequency data
      const value = this.dataArray[i * step]
      const barHeight = (value / 255) * height * 0.8

      const x = i * totalBarWidth
      const y = (height - barHeight) / 2

      // Draw bar with rounded corners
      this.canvasCtx.beginPath()
      this.canvasCtx.roundRect(x, y, this.barWidth, barHeight, this.barWidth / 2)
      this.canvasCtx.fill()
    }
  }

  disconnect(): void {
    if (this.animationId) {
      cancelAnimationFrame(this.animationId)
      this.animationId = null
    }

    if (this.source) {
      this.source.disconnect()
      this.source = null
    }

    if (this.audioContext) {
      this.audioContext.close()
      this.audioContext = null
    }

    this.analyser = null
    this.dataArray = null

    // Clear canvas
    const width = this.canvas.width
    const height = this.canvas.height
    this.canvasCtx.fillStyle = this.backgroundColor
    this.canvasCtx.fillRect(0, 0, width, height)
  }

  /**
   * Get current audio level (0-1)
   */
  getAudioLevel(): number {
    if (!this.analyser || !this.dataArray) return 0

    this.analyser.getByteFrequencyData(this.dataArray)

    let sum = 0
    for (let i = 0; i < this.dataArray.length; i++) {
      sum += this.dataArray[i]
    }

    return sum / (this.dataArray.length * 255)
  }
}

/**
 * Draw a static waveform for playback visualization
 */
export function drawStaticWaveform(
  canvas: HTMLCanvasElement,
  audioBuffer: AudioBuffer,
  options: {
    barColor?: string
    backgroundColor?: string
    barWidth?: number
    barGap?: number
    progress?: number
    progressColor?: string
  } = {}
): void {
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const {
    barColor = '#c0c4cc',
    backgroundColor = '#f5f7fa',
    barWidth = 2,
    barGap = 1,
    progress = 0,
    progressColor = '#409eff',
  } = options

  const width = canvas.width
  const height = canvas.height
  const channelData = audioBuffer.getChannelData(0)

  // Clear canvas
  ctx.fillStyle = backgroundColor
  ctx.fillRect(0, 0, width, height)

  const totalBarWidth = barWidth + barGap
  const numBars = Math.floor(width / totalBarWidth)
  const samplesPerBar = Math.floor(channelData.length / numBars)

  for (let i = 0; i < numBars; i++) {
    // Calculate RMS for this segment
    let sum = 0
    const start = i * samplesPerBar
    for (let j = 0; j < samplesPerBar; j++) {
      const sample = channelData[start + j] || 0
      sum += sample * sample
    }
    const rms = Math.sqrt(sum / samplesPerBar)
    const barHeight = Math.max(2, rms * height * 2)

    const x = i * totalBarWidth
    const y = (height - barHeight) / 2
    const progressX = width * progress

    // Set color based on progress
    ctx.fillStyle = x < progressX ? progressColor : barColor

    ctx.beginPath()
    ctx.roundRect(x, y, barWidth, barHeight, barWidth / 2)
    ctx.fill()
  }
}
