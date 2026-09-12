import { useEffect, useRef } from 'react'
import * as echarts from 'echarts'

interface ChartViewProps {
  option: Record<string, unknown>
}

export function ChartView({ option }: ChartViewProps) {
  const elRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const el = elRef.current
    if (!el) {
      return
    }
    const chart = echarts.init(el)
    chart.setOption(option)
    const onResize = () => chart.resize()
    window.addEventListener('resize', onResize)
    return () => {
      window.removeEventListener('resize', onResize)
      chart.dispose()
    }
  }, [option])

  return <div className="chart-view" ref={elRef} />
}
