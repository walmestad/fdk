import _ from 'lodash';
import React from 'react';
import PropTypes from 'prop-types';
import sanitizeHtml from 'sanitize-html';

import localization from '../../../lib/localization';
import { ShowMore } from '../../../components/show-more/show-more';
import { HarvestDate } from '../../../components/harvest-date/harvest-date.component';
import { SearchHitHeader } from '../../../components/search-hit-header/search-hit-header.component';
import { getTranslateText } from '../../../lib/translateText';

export class DatasetDescription extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      showAll: false
    };
    this.toggleShowAll = this.toggleShowAll.bind(this);
  }

  toggleShowAll() {
    this.setState({ showAll: !this.state.showAll });
  }

  render() {
    const { datasetItem } = this.props;
    const { harvest, publisher, theme, provenance } = datasetItem || {};
    let { title, descriptionFormatted, objective } = datasetItem || {};
    title = getTranslateText(title);
    descriptionFormatted = getTranslateText(descriptionFormatted);
    objective = getTranslateText(objective);

    return (
      <header>
        <div className="fdk-detail-date mb-5">
          <HarvestDate harvest={harvest} />
        </div>

        <SearchHitHeader
          title={title}
          publisherLabel={localization.search_hit.owned}
          publisher={publisher}
          theme={theme}
          nationalComponent={_.get(provenance, 'code') === 'NASJONAL'}
        />

        {descriptionFormatted && (
          <ShowMore showMoreButtonText={localization.showFullDescription}>
            <span
              dangerouslySetInnerHTML={{
                __html: sanitizeHtml(descriptionFormatted)
              }}
            />
          </ShowMore>
        )}

        {objective && (
          <ShowMore showMoreButtonText={localization.showFullObjective}>
            <strong>{localization.objective}: </strong>
            {objective}
          </ShowMore>
        )}
      </header>
    );
  }
}

DatasetDescription.defaultProps = {
  datasetItem: null
};

DatasetDescription.propTypes = {
  datasetItem: PropTypes.object
};
