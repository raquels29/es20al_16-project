<!-- <template>
  <v-dialog
    :value="dialog"
    @input="$emit('dialog', false)"
    @keydown.esc="$emit('dialog', false)"
    max-width="55%"
    max-height="80%"
  >
    <v-card>
      <v-container grid-list-md fluid>
        <v-layout column wrap>
      <v-card-title>
        <span class="headline"> {{ 'Edit Evaluation' }} </span>
      </v-card-title>
          <v-col cols="12">
      <v-select
              outlined
              v-model="evaluate.evaluation"
              :items="evaluationsList"
              :color="getEvaluationColor(evaluate.evaluation)" small>
            <span>{{evaluate.evaluation}}</span>
      </v-select>
            </v-col>

      <v-card-text class="text-left" v-if="evaluate">
        <v-flex xs24 sm12 md12>
          <v-textarea
           outline
           rows="5"
           v-model="evaluate.justification"
           label="Justification"
         ></v-textarea>
        </v-flex>
      </v-card-text>


      <v-card-actions>
        <v-spacer />
        <v-btn color="blue darken-1" @click="$emit('dialog', false)"
          >Cancel</v-btn
        >
        <v-btn color="blue darken-1" @click="saveEvaluation">Save</v-btn>
      </v-card-actions>
        </v-layout>
      </v-container>
    </v-card>
  </v-dialog>
</template>

<script lang="ts">
import { Component, Model, Prop, Vue } from 'vue-property-decorator';
import RemoteServices from '@/services/RemoteServices';
import ProposedQuestion from '@/models/management/ProposedQuestion';
import Store from '@/store';


@Component
export default class EditJustificationDialog extends Vue {
  @Model('dialog', Boolean) dialog!: boolean;
  @Prop({ type: ProposedQuestion, required: true }) readonly evaluate!: ProposedQuestion;
  evaluationsList = ['AWAITING', 'APPROVED', 'REJECTED'];

  async saveEvaluation() {
    this.evaluate.teacher = Store.getters.getUser;
    try {
      const result = await RemoteServices.evaluate(this.evaluate);
      this.$emit('save-evaluation', result);
    } catch (error) {
      await this.$store.dispatch('error', error);
    }
  }

  getEvaluationColor(evaluation: string) {
    if (evaluation === 'AWAITING') return 'grey';
    else if (evaluation === 'APPROVED') return 'green';
    else return 'red';
  }
}
</script> -->