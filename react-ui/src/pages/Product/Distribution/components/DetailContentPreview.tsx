import { Image } from 'antd';
import { parseDetailContent } from '../detailContent';
import { resolveResourceUrl } from '../constants';
import styles from '../style.module.css';

type DetailContentPreviewProps = {
  value?: string;
};

export default function DetailContentPreview({ value }: DetailContentPreviewProps) {
  const blocks = parseDetailContent(value);

  if (!blocks.length) {
    return <span className={styles.mutedText}>--</span>;
  }

  return (
    <div className={styles.detailPreview}>
      {blocks.map((block) => {
        if (block.type === 'TEXT') {
          return <p key={block.id}>{block.text || '--'}</p>;
        }
        if (block.type === 'IMAGE') {
          return block.imageUrl ? (
            <Image key={block.id} width={160} src={resolveResourceUrl(block.imageUrl)} />
          ) : <div key={block.id} className={styles.mutedText}>图片未上传</div>;
        }
        if (block.type === 'IMAGE_TEXT') {
          return (
            <div className={styles.detailPreviewImageText} key={block.id}>
              {block.imageUrl ? <Image width={120} src={resolveResourceUrl(block.imageUrl)} /> : null}
              <div>
                {block.title ? <div className={styles.detailPreviewTitle}>{block.title}</div> : null}
                <p>{block.text || '--'}</p>
              </div>
            </div>
          );
        }
        return (
          <div className={styles.detailPreviewParams} key={block.id}>
            {(block.rows || []).map((row) => (
              <div className={styles.detailPreviewParamRow} key={row.id}>
                <span>{row.name || '--'}</span>
                <span>{row.value || '--'}</span>
              </div>
            ))}
          </div>
        );
      })}
    </div>
  );
}
