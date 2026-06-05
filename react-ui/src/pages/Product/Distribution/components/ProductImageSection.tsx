import ImageUploadField from './ImageUploadField';
import styles from '../style.module.css';

type ProductImageSectionProps = {
  mainImageUrl?: string;
  galleryUrls: string[];
  onMainImageChange: (value?: string) => void;
  onGalleryChange: (value: string[]) => void;
};

function updateAt(values: string[], index: number, value?: string) {
  const next = [...values];
  next[index] = value || '';
  return next;
}

export default function ProductImageSection({
  mainImageUrl,
  galleryUrls,
  onMainImageChange,
  onGalleryChange,
}: ProductImageSectionProps) {
  const normalizedGalleryUrls = Array.from({ length: 7 }, (_, index) => galleryUrls[index] || '');

  return (
    <div className={styles.imageSection}>
      <div className={styles.imageSectionHeader}>
        <div>
          <div className={styles.sectionTitle}>商品图片</div>
          <div className={styles.sectionHint}>建议主图清晰展示商品主体；轮播图用于列表和详情补充展示。</div>
        </div>
      </div>
      <div className={styles.imageGrid}>
        <ImageUploadField
          label="主图"
          required
          value={mainImageUrl}
          onChange={onMainImageChange}
        />
        {normalizedGalleryUrls.map((url, index) => (
          <ImageUploadField
            key={`gallery-${index}`}
            label={index === 0 ? '尺寸图' : undefined}
            reserveLabelSpace
            value={url}
            onChange={(value) => onGalleryChange(updateAt(normalizedGalleryUrls, index, value))}
          />
        ))}
      </div>
    </div>
  );
}
