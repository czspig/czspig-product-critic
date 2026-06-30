import type { GoDecision, ReviewDetailResponse } from '@/types/review';

export interface ShareCardData {
  title: string;
  decision: string;
  beatScore: number;
  positioningScore: number;
  oneLineVerdict: string;
  successMetric: string;
  versionLabel: string;
}

const decisionLabels: Record<GoDecision, string> = {
  CONTINUE: '建议继续',
  PIVOT: '建议调整方向',
  PAUSE: '建议暂缓',
};

export function decisionLabel(decision?: GoDecision) {
  return decision ? decisionLabels[decision] : '建议继续';
}

export function toShareCardData(review: ReviewDetailResponse): ShareCardData {
  return {
    title: review.inputSummary || truncate(review.inputContent || '产品想法', 48),
    decision: decisionLabel(review.report?.goDecision),
    beatScore: review.beatScore || 0,
    positioningScore: review.positioningScore || 0,
    oneLineVerdict: review.oneLineVerdict || '这份产品想法还需要继续打磨。',
    successMetric: review.report?.minimumBuildVersion?.successMetric || '先用最小实验验证用户是否愿意持续使用。',
    versionLabel: review.versionNo ? `V${review.versionNo}` : '',
  };
}

export function buildShareCardText(review: ReviewDetailResponse) {
  const data = toShareCardData(review);
  return `【猪猪产品毒舌官·产品体检卡】
产品想法：${data.title}
决策结论：${data.decision}
毒打指数：${data.beatScore}/100
产品定位评分：${data.positioningScore}/100
一句话评价：${data.oneLineVerdict}
成功指标：${data.successMetric}
由「猪猪产品毒舌官」生成`;
}

export async function downloadShareCardPng(review: ReviewDetailResponse) {
  const data = toShareCardData(review);
  const canvas = document.createElement('canvas');
  canvas.width = 1080;
  canvas.height = 1440;
  const ctx = canvas.getContext('2d');
  if (!ctx) {
    throw new Error('当前浏览器不支持生成图片');
  }

  drawCard(ctx, data);
  const url = canvas.toDataURL('image/png');
  const date = review.createdAt?.slice(0, 10).replaceAll('-', '') || 'review';
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = `czspig-health-card-${date}-${review.id}.png`;
  document.body.appendChild(anchor);
  anchor.click();
  document.body.removeChild(anchor);
}

function drawCard(ctx: CanvasRenderingContext2D, data: ShareCardData) {
  ctx.fillStyle = '#f6efe3';
  ctx.fillRect(0, 0, 1080, 1440);
  drawPaperTexture(ctx);

  ctx.fillStyle = '#fffaf0';
  ctx.strokeStyle = '#71675b';
  ctx.lineWidth = 6;
  roundRect(ctx, 80, 78, 920, 1284, 18);
  ctx.fill();
  ctx.stroke();

  drawPixelPig(ctx, 795, 120, 1.55);

  drawStamp(ctx, 96, 116, '产品想法体检卡', '#a6402d', -0.05);
  if (data.versionLabel) {
    drawStamp(ctx, 94, 180, data.versionLabel, '#47695f', 0.04);
  }

  drawText(ctx, '猪猪产品毒舌官', 100, 292, 34, '#842b20', 900, 820);
  drawWrapped(ctx, data.title, 100, 360, 760, 58, 46, '#26231f', 900, 3);

  drawMetric(ctx, 116, 575, '决策结论', data.decision, '#9b6a2b');
  drawMetric(ctx, 116, 720, '毒打指数', `${data.beatScore}/100`, '#a6402d');
  drawMetric(ctx, 550, 720, '产品定位评分', `${data.positioningScore}/100`, '#47695f');

  drawSection(ctx, 116, 905, '一句话评价', data.oneLineVerdict);
  drawSection(ctx, 116, 1095, '成功指标', data.successMetric);

  ctx.fillStyle = '#a6402d';
  ctx.fillRect(116, 1286, 848, 4);
  drawText(ctx, '由「猪猪产品毒舌官」生成', 116, 1335, 26, '#766d61', 800, 820);
}

function drawPaperTexture(ctx: CanvasRenderingContext2D) {
  ctx.fillStyle = 'rgba(113, 103, 91, 0.08)';
  for (let x = 0; x < 1080; x += 28) {
    for (let y = 0; y < 1440; y += 28) {
      ctx.fillRect(x + 2, y + 2, 2, 2);
    }
  }
}

function drawMetric(ctx: CanvasRenderingContext2D, x: number, y: number, label: string, value: string, color: string) {
  ctx.fillStyle = '#f2e8d7';
  ctx.strokeStyle = '#71675b';
  ctx.lineWidth = 4;
  roundRect(ctx, x, y, 366, 118, 10);
  ctx.fill();
  ctx.stroke();
  drawText(ctx, label, x + 28, y + 42, 24, '#766d61', 900, 300);
  drawText(ctx, value, x + 28, y + 92, 38, color, 900, 310);
}

function drawSection(ctx: CanvasRenderingContext2D, x: number, y: number, title: string, body: string) {
  ctx.fillStyle = '#a6402d';
  ctx.fillRect(x, y, 8, 104);
  drawText(ctx, title, x + 24, y + 28, 24, '#a6402d', 900, 760);
  drawWrapped(ctx, body, x + 24, y + 78, 790, 30, 32, '#26231f', 800, 3);
}

function drawStamp(ctx: CanvasRenderingContext2D, x: number, y: number, text: string, color: string, rotation: number) {
  ctx.save();
  ctx.translate(x, y);
  ctx.rotate(rotation);
  ctx.strokeStyle = color;
  ctx.lineWidth = 4;
  ctx.strokeRect(0, 0, Math.max(156, text.length * 29), 44);
  drawText(ctx, text, 14, 31, 22, color, 900, 360);
  ctx.restore();
}

function drawPixelPig(ctx: CanvasRenderingContext2D, x: number, y: number, scale: number) {
  ctx.save();
  ctx.translate(x, y);
  ctx.scale(scale, scale);
  ctx.fillStyle = '#f2a3a7';
  ctx.strokeStyle = '#332923';
  ctx.lineWidth = 4;
  ctx.fillRect(18, 18, 78, 70);
  ctx.strokeRect(18, 18, 78, 70);
  ctx.fillStyle = '#f7c2c0';
  ctx.fillRect(42, 58, 30, 20);
  ctx.strokeRect(42, 58, 30, 20);
  ctx.fillStyle = '#332923';
  ctx.fillRect(38, 43, 8, 8);
  ctx.fillRect(70, 43, 8, 8);
  ctx.fillRect(50, 66, 5, 6);
  ctx.fillRect(61, 66, 5, 6);
  ctx.fillStyle = '#58534d';
  ctx.fillRect(20, 88, 78, 52);
  ctx.strokeRect(20, 88, 78, 52);
  ctx.fillStyle = '#a6402d';
  ctx.fillRect(52, 96, 14, 32);
  ctx.restore();
}

function drawText(
  ctx: CanvasRenderingContext2D,
  text: string,
  x: number,
  y: number,
  size: number,
  color: string,
  weight: number,
  maxWidth: number,
) {
  ctx.fillStyle = color;
  ctx.font = `${weight} ${size}px "Microsoft YaHei", "PingFang SC", Arial, sans-serif`;
  ctx.fillText(text, x, y, maxWidth);
}

function drawWrapped(
  ctx: CanvasRenderingContext2D,
  text: string,
  x: number,
  y: number,
  maxWidth: number,
  lineHeight: number,
  size: number,
  color: string,
  weight: number,
  maxLines: number,
) {
  ctx.fillStyle = color;
  ctx.font = `${weight} ${size}px "Microsoft YaHei", "PingFang SC", Arial, sans-serif`;
  const lines = wrapText(ctx, text, maxWidth, maxLines);
  lines.forEach((line, index) => {
    ctx.fillText(line, x, y + index * lineHeight, maxWidth);
  });
}

function wrapText(ctx: CanvasRenderingContext2D, text: string, maxWidth: number, maxLines: number) {
  const chars = Array.from(text);
  const lines: string[] = [];
  let line = '';
  for (const char of chars) {
    const next = line + char;
    if (ctx.measureText(next).width > maxWidth && line) {
      lines.push(line);
      line = char;
      if (lines.length === maxLines - 1) {
        break;
      }
    } else {
      line = next;
    }
  }
  if (line && lines.length < maxLines) {
    lines.push(line);
  }
  if (lines.length === maxLines && chars.join('').length > lines.join('').length) {
    lines[maxLines - 1] = `${lines[maxLines - 1].slice(0, Math.max(0, lines[maxLines - 1].length - 1))}...`;
  }
  return lines;
}

function roundRect(ctx: CanvasRenderingContext2D, x: number, y: number, width: number, height: number, radius: number) {
  ctx.beginPath();
  ctx.moveTo(x + radius, y);
  ctx.lineTo(x + width - radius, y);
  ctx.quadraticCurveTo(x + width, y, x + width, y + radius);
  ctx.lineTo(x + width, y + height - radius);
  ctx.quadraticCurveTo(x + width, y + height, x + width - radius, y + height);
  ctx.lineTo(x + radius, y + height);
  ctx.quadraticCurveTo(x, y + height, x, y + height - radius);
  ctx.lineTo(x, y + radius);
  ctx.quadraticCurveTo(x, y, x + radius, y);
  ctx.closePath();
}

function truncate(value: string, maxLength: number) {
  return value.length <= maxLength ? value : `${value.slice(0, maxLength)}...`;
}
